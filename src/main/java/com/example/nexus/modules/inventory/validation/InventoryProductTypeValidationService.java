package com.example.nexus.modules.inventory.validation;

import com.example.nexus.exception.ValidationException;
import com.example.nexus.modules.inventory.dto.request.CreateLotRequestDTO;
import com.example.nexus.modules.inventory.entity.Lot;
import com.example.nexus.modules.inventory.entity.Product;
import com.example.nexus.modules.inventory.entity.ProductTypeConfig;
import com.example.nexus.modules.inventory.repository.LotRepository;
import com.example.nexus.modules.inventory.service.ProductTypeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validaciones dinámicas de lote y movimiento según {@link ProductTypeConfig}.
 */
@Component
@RequiredArgsConstructor
public class InventoryProductTypeValidationService {

    /**
     * Lote técnico único por producto cuando {@code requires_lot = false} (FK en movimientos/inventario).
     * No debe crearse vía API de lotes; se genera bajo demanda.
     */
    public static final String INTERNAL_SINGLE_LOT_MARKER = "__SIN_LOTE__";

    private final ProductTypeConfigService productTypeConfigService;
    private final LotRepository lotRepository;

    public ProductTypeConfig requireConfig(Product product) {
        if (product.getProductType() == null || product.getProductType().isBlank()) {
            throw new ValidationException("El producto debe tener un tipo de producto (productType) configurado");
        }
        return productTypeConfigService.getConfigByProductType(product.getProductType());
    }

    public void validateCreateLot(Product product, CreateLotRequestDTO dto) {
        ProductTypeConfig config = requireConfig(product);
        if (Boolean.FALSE.equals(config.getRequiresLot())) {
            throw new ValidationException("Este tipo de producto no maneja lotes");
        }
        CreateLotRequestDTO body = dto != null ? dto : new CreateLotRequestDTO(null, null, null);
        if (Boolean.TRUE.equals(config.getHasExpiration())) {
            if (body.expirationDate() == null) {
                throw new ValidationException(
                        "El tipo " + config.getProductType() + " requiere fecha de vencimiento");
            }
        } else {
            if (body.expirationDate() != null) {
                throw new ValidationException(
                        "El tipo " + config.getProductType() + " no maneja fecha de vencimiento");
            }
        }
    }

    /**
     * @param requestedLotId {@code null} solo si el tipo no requiere lote; en ese caso se usa el lote interno.
     */
    public Lot resolveLotForMovement(Product product, Long requestedLotId) {
        ProductTypeConfig config = requireConfig(product);
        if (Boolean.TRUE.equals(config.getRequiresLot())) {
            if (requestedLotId == null) {
                throw new ValidationException(
                        "El tipo " + config.getProductType() + " requiere lote (lotId obligatorio)");
            }
            Lot lot = lotRepository.findById(requestedLotId)
                    .orElseThrow(() -> new ValidationException("Lote no encontrado"));
            if (!lot.getProduct().getId().equals(product.getId())) {
                throw new ValidationException("El lote no pertenece al producto indicado");
            }
            if (INTERNAL_SINGLE_LOT_MARKER.equals(lot.getLotNumber())) {
                throw new ValidationException("Lote inválido para este producto");
            }
            return lot;
        }
        if (requestedLotId != null) {
            throw new ValidationException(
                    "El tipo " + config.getProductType() + " no requiere lote (no envíe lotId)");
        }
        return getOrCreateInternalSingleLot(product);
    }

    public boolean isInternalPlaceholderLot(Lot lot) {
        return lot != null && INTERNAL_SINGLE_LOT_MARKER.equals(lot.getLotNumber());
    }

    private Lot getOrCreateInternalSingleLot(Product product) {
        return lotRepository
                .findByProduct_IdAndLotNumber(product.getId(), INTERNAL_SINGLE_LOT_MARKER)
                .orElseGet(() -> lotRepository.save(Lot.builder()
                        .product(product)
                        .lotNumber(INTERNAL_SINGLE_LOT_MARKER)
                        .expirationDate(null)
                        .productionDate(null)
                        .build()));
    }
}
