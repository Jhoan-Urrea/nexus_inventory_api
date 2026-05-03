package com.example.nexus.modules.inventory.service;

import com.example.nexus.exception.ValidationException;
import com.example.nexus.modules.auth.security.CurrentUserProvider;
import com.example.nexus.modules.inventory.dto.request.*;
import com.example.nexus.modules.inventory.dto.response.*;
import com.example.nexus.modules.inventory.entity.*;
import com.example.nexus.modules.inventory.repository.*;
import com.example.nexus.modules.inventory.util.BarcodeValidator;
import com.example.nexus.modules.inventory.validation.InventoryProductTypeValidationService;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.modules.warehouse.entity.Sector;
import com.example.nexus.modules.warehouse.entity.StorageSpace;
import com.example.nexus.modules.warehouse.repository.SectorRepository;
import com.example.nexus.modules.warehouse.repository.StorageSpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryProcessServiceImpl implements InventoryProcessService {

    private static final PageRequest RECENT_PAGE = PageRequest.of(0, 100);
    private static final String COUNT_OPEN = "OPEN";
    private static final String COUNT_COMPLETED = "COMPLETED";

    private final ProductRepository productRepository;
    private final LotRepository lotRepository;
    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final MovementTypeRepository movementTypeRepository;
    private final MovementSubtypeRepository movementSubtypeRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final InventoryAlertRepository inventoryAlertRepository;
    private final InventoryCountRepository inventoryCountRepository;
    private final InventoryCountDetailRepository inventoryCountDetailRepository;
    private final StorageSpaceRepository storageSpaceRepository;
    private final SectorRepository sectorRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ProductTypeConfigService productTypeConfigService;
    private final InventoryProductTypeValidationService inventoryProductTypeValidationService;

    @Override
    @Transactional
    public InventoryProductResponseDTO createProduct(CreateInventoryProductRequestDTO request) {
        BarcodeValidator.assertBarcodeValid(request.barcode());
        String barcode = BarcodeValidator.normalizeBarcode(request.barcode());
        if (productRepository.existsByBarcode(barcode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un producto con ese código de barras");
        }
        String normalizedType = productTypeConfigService.normalizeProductType(request.productType());
        productTypeConfigService.assertProductTypeConfigured(normalizedType);
        Product entity = Product.builder()
                .name(request.name().trim())
                .barcode(barcode)
                .productType(normalizedType)
                .unit(request.unit())
                .active(true)
                .build();
        Product saved = productRepository.save(entity);
        return toProductDto(saved);
    }

    @Override
    public List<InventoryProductResponseDTO> findAllProducts() {
        return productRepository.findAll().stream().map(this::toProductDto).toList();
    }

    @Override
    @Transactional
    public LotResponseDTO createLot(Long productId, CreateLotRequestDTO request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        CreateLotRequestDTO dto = request != null ? request : new CreateLotRequestDTO(null, null, null);
        inventoryProductTypeValidationService.validateCreateLot(product, dto);
        Lot lot = Lot.builder()
                .product(product)
                .lotNumber(dto.lotNumber() != null ? dto.lotNumber().trim() : null)
                .expirationDate(dto.expirationDate())
                .productionDate(dto.productionDate())
                .build();
        return toLotDto(lotRepository.save(lot));
    }

    @Override
    public List<LotResponseDTO> findLotsByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
        }
        return lotRepository.findByProductIdOrderByIdAsc(productId).stream()
                .filter(l -> !InventoryProductTypeValidationService.INTERNAL_SINGLE_LOT_MARKER.equals(l.getLotNumber()))
                .map(this::toLotDto)
                .toList();
    }

    @Override
    public List<MovementTypeResponseDTO> findMovementTypes() {
        return movementTypeRepository.findAll().stream()
                .map(t -> new MovementTypeResponseDTO(t.getId(), t.getName(), t.getDescription()))
                .toList();
    }

    @Override
    public List<MovementSubtypeResponseDTO> findMovementSubtypes(Long typeId) {
        if (!movementTypeRepository.existsById(typeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de movimiento no encontrado");
        }
        return movementSubtypeRepository.findByMovementType_IdOrderByIdAsc(typeId).stream()
                .map(s -> new MovementSubtypeResponseDTO(
                        s.getId(),
                        s.getMovementType() != null ? s.getMovementType().getId() : typeId,
                        s.getName(),
                        s.getDescription()))
                .toList();
    }

    @Override
    public List<InventoryBalanceResponseDTO> findBalances(Long storageSpaceId, Long productId) {
        return inventoryBalanceRepository.findFiltered(storageSpaceId, productId).stream()
                .map(this::toBalanceDto)
                .toList();
    }

    @Override
    @Transactional
    public InventoryMovementResponseDTO registerMovement(RegisterInventoryMovementRequestDTO request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        Lot lot = inventoryProductTypeValidationService.resolveLotForMovement(product, request.lotId());
        StorageSpace storage = storageSpaceRepository.findById(request.storageSpaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio de almacenamiento no encontrado"));
        MovementType type = movementTypeRepository.findById(request.movementTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de movimiento no encontrado"));
        MovementSubtype subtype = null;
        if (request.movementSubtypeId() != null) {
            subtype = movementSubtypeRepository.findById(request.movementSubtypeId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subtipo de movimiento no encontrado"));
            if (subtype.getMovementType() == null || !subtype.getMovementType().getId().equals(type.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El subtipo no corresponde al tipo de movimiento");
            }
        }

        Long userId = currentUserProvider.getCurrentUserId();
        AppUser userRef = appUserRepository.getReferenceById(userId);

        InventoryMovement movement = InventoryMovement.builder()
                .product(product)
                .lot(lot)
                .storageSpace(storage)
                .user(userRef)
                .movementType(type)
                .movementSubtype(subtype)
                .quantity(request.quantity())
                .note(request.note())
                .build();
        InventoryMovement saved = inventoryMovementRepository.save(movement);
        return toMovementDto(saved);
    }

    @Override
    public List<InventoryMovementResponseDTO> findRecentMovements() {
        return inventoryMovementRepository.findRecentWithRelations(RECENT_PAGE).stream()
                .map(this::toMovementDto)
                .toList();
    }

    @Override
    public List<InventoryHistoryResponseDTO> findRecentHistory() {
        return inventoryHistoryRepository.findRecentWithMovement(RECENT_PAGE).stream()
                .map(h -> new InventoryHistoryResponseDTO(
                        h.getId(),
                        h.getMovement().getId(),
                        h.getQuantityBefore(),
                        h.getQuantityAfter(),
                        h.getCreatedAt()))
                .toList();
    }

    @Override
    public List<InventoryAlertResponseDTO> findAlerts(boolean openOnly) {
        List<InventoryAlert> list = openOnly
                ? inventoryAlertRepository.findByResolvedOrderByIdDesc(false)
                : inventoryAlertRepository.findAllByOrderByIdDesc();
        return list.stream().map(this::toAlertDto).toList();
    }

    @Override
    @Transactional
    public void resolveAlert(Long alertId) {
        InventoryAlert alert = inventoryAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alerta no encontrada"));
        alert.setResolved(true);
    }

    @Override
    @Transactional
    public InventoryCountResponseDTO startInventoryCount(CreateInventoryCountRequestDTO request) {
        Long userId = currentUserProvider.getCurrentUserId();
        AppUser user = appUserRepository.getReferenceById(userId);
        Sector sector = null;
        if (request.sectorId() != null) {
            sector = sectorRepository.findById(request.sectorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sector no encontrado"));
        }
        InventoryCount count = InventoryCount.builder()
                .sector(sector)
                .user(user)
                .startedAt(LocalDateTime.now())
                .status(COUNT_OPEN)
                .build();
        return toCountDto(inventoryCountRepository.save(count));
    }

    @Override
    public List<InventoryCountResponseDTO> findAllCounts() {
        return inventoryCountRepository.findAllByOrderByIdDesc().stream().map(this::toCountDto).toList();
    }

    @Override
    @Transactional
    public InventoryCountDetailResponseDTO addCountLine(Long countId, AddInventoryCountLineRequestDTO request) {
        InventoryCount count = inventoryCountRepository.findById(countId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteo no encontrado"));
        if (COUNT_COMPLETED.equalsIgnoreCase(count.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El conteo ya está cerrado");
        }
        if (request.productId() == null || request.storageSpaceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId y storageSpaceId son obligatorios");
        }
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        StorageSpace space = storageSpaceRepository.findById(request.storageSpaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio de almacenamiento no encontrado"));

        ProductTypeConfig config = inventoryProductTypeValidationService.requireConfig(product);
        Lot lot = null;
        if (Boolean.TRUE.equals(config.getRequiresLot())) {
            if (request.lotId() == null) {
                throw new ValidationException(
                        "El tipo " + config.getProductType() + " requiere lote en la línea de conteo");
            }
            lot = lotRepository.findById(request.lotId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado"));
            if (!lot.getProduct().getId().equals(product.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El lote no pertenece al producto");
            }
            if (InventoryProductTypeValidationService.INTERNAL_SINGLE_LOT_MARKER.equals(lot.getLotNumber())) {
                throw new ValidationException("Lote inválido para la línea de conteo");
            }
        } else if (request.lotId() != null) {
            throw new ValidationException(
                    "El tipo " + config.getProductType() + " no requiere lote (no envíe lotId)");
        }
        InventoryCountDetail line = InventoryCountDetail.builder()
                .inventoryCount(count)
                .product(product)
                .lot(lot)
                .storageSpace(space)
                .systemQty(request.systemQty())
                .physicalQty(request.physicalQty())
                .difference(request.difference())
                .build();
        InventoryCountDetail saved = inventoryCountDetailRepository.save(line);
        return toCountLineDto(saved);
    }

    @Override
    public List<InventoryCountDetailResponseDTO> findCountLines(Long countId) {
        if (!inventoryCountRepository.existsById(countId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteo no encontrado");
        }
        return inventoryCountDetailRepository.findByInventoryCount_IdOrderByIdAsc(countId).stream()
                .map(this::toCountLineDto)
                .toList();
    }

    @Override
    @Transactional
    public InventoryCountResponseDTO completeCount(Long countId) {
        InventoryCount count = inventoryCountRepository.findById(countId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteo no encontrado"));
        if (COUNT_COMPLETED.equalsIgnoreCase(count.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El conteo ya estaba cerrado");
        }
        count.setStatus(COUNT_COMPLETED);
        count.setFinishedAt(LocalDateTime.now());
        return toCountDto(count);
    }

    private InventoryProductResponseDTO toProductDto(Product p) {
        return new InventoryProductResponseDTO(
                p.getId(), p.getName(), p.getBarcode(), p.getProductType(), p.getUnit(), p.getActive(), p.getCreatedAt());
    }

    private LotResponseDTO toLotDto(Lot l) {
        return new LotResponseDTO(
                l.getId(),
                l.getProduct().getId(),
                l.getLotNumber(),
                l.getExpirationDate(),
                l.getProductionDate(),
                l.getCreatedAt());
    }

    private InventoryBalanceResponseDTO toBalanceDto(InventoryBalance b) {
        return new InventoryBalanceResponseDTO(
                b.getId(),
                b.getProduct().getId(),
                b.getProduct().getName(),
                b.getLot().getId(),
                b.getLot().getLotNumber(),
                b.getStorageSpace().getId(),
                b.getStorageSpace().getCode(),
                b.getQuantity(),
                b.getUpdatedAt());
    }

    private InventoryMovementResponseDTO toMovementDto(InventoryMovement m) {
        return new InventoryMovementResponseDTO(
                m.getId(),
                m.getProduct().getId(),
                m.getLot().getId(),
                m.getStorageSpace().getId(),
                m.getUser() != null ? m.getUser().getId() : null,
                m.getMovementType() != null ? m.getMovementType().getId() : null,
                m.getMovementType() != null ? m.getMovementType().getName() : null,
                m.getMovementSubtype() != null ? m.getMovementSubtype().getId() : null,
                m.getMovementSubtype() != null ? m.getMovementSubtype().getName() : null,
                m.getQuantity(),
                m.getNote(),
                m.getCreatedAt());
    }

    private InventoryAlertResponseDTO toAlertDto(InventoryAlert a) {
        return new InventoryAlertResponseDTO(
                a.getId(),
                a.getProduct() != null ? a.getProduct().getId() : null,
                a.getLot() != null ? a.getLot().getId() : null,
                a.getStorageSpace() != null ? a.getStorageSpace().getId() : null,
                a.getAlertType(),
                a.getCurrentQuantity(),
                a.getResolved(),
                a.getCreatedAt());
    }

    private InventoryCountResponseDTO toCountDto(InventoryCount c) {
        return new InventoryCountResponseDTO(
                c.getId(),
                c.getSector() != null ? c.getSector().getId() : null,
                c.getUser() != null ? c.getUser().getId() : null,
                c.getStartedAt(),
                c.getFinishedAt(),
                c.getStatus());
    }

    private InventoryCountDetailResponseDTO toCountLineDto(InventoryCountDetail d) {
        return new InventoryCountDetailResponseDTO(
                d.getId(),
                d.getInventoryCount().getId(),
                d.getProduct() != null ? d.getProduct().getId() : null,
                d.getLot() != null ? d.getLot().getId() : null,
                d.getStorageSpace() != null ? d.getStorageSpace().getId() : null,
                d.getSystemQty(),
                d.getPhysicalQty(),
                d.getDifference());
    }
}
