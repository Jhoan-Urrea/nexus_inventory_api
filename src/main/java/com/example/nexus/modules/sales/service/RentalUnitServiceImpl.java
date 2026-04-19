package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.dto.request.UpdateRentalUnitPricingRequestDTO;
import com.example.nexus.modules.sales.dto.response.RentalUnitPricingDTO;
import com.example.nexus.modules.sales.entity.RentalUnit;
import com.example.nexus.modules.sales.repository.RentalUnitRepository;
import com.example.nexus.modules.user.entity.AppUser;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalUnitServiceImpl implements RentalUnitService {

    private final RentalUnitRepository rentalUnitRepository;
    private final AppUserRepository appUserRepository;

    @Override
    public RentalUnit create(RentalUnit rentalUnit) {
        return rentalUnitRepository.save(rentalUnit);
    }

    @Override
    public List<RentalUnit> findAll() {
        return rentalUnitRepository.findAll();
    }

    @Override
    public RentalUnit findById(Long id) {
        return rentalUnitRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental unit not found"));
    }

    @Override
    public RentalUnit update(Long id, RentalUnit rentalUnit) {
        RentalUnit existing = findById(id);
        existing.setWarehouse(rentalUnit.getWarehouse());
        existing.setSector(rentalUnit.getSector());
        existing.setStorageSpace(rentalUnit.getStorageSpace());
        existing.setEntityType(rentalUnit.getEntityType());
        return rentalUnitRepository.save(existing);
    }

    @Override
    public List<RentalUnitPricingDTO> findPricingCatalog(Boolean readyOnly, Boolean activeOnly) {
        return rentalUnitRepository.findAllByOrderByIdAsc().stream()
                .filter(ru -> !Boolean.TRUE.equals(readyOnly) || isReadyForCommercialUse(ru))
                .filter(ru -> activeOnly == null || activeOnly.equals(Boolean.TRUE.equals(ru.getPriceActive())))
                .map(this::toPricingDTO)
                .toList();
    }

    @Override
    public RentalUnitPricingDTO updatePricing(Long id, UpdateRentalUnitPricingRequestDTO request, String actorEmail) {
        RentalUnit existing = findById(id);
        validateCurrency(request.currency());
        existing.setBasePrice(request.basePrice());
        existing.setCurrency(request.currency().trim().toUpperCase());
        existing.setPriceActive(request.priceActive());
        existing.setPriceUpdatedAt(LocalDateTime.now());
        existing.setPriceUpdatedBy(resolveActorId(actorEmail));
        return toPricingDTO(rentalUnitRepository.save(existing));
    }

    @Override
    public void delete(Long id) {
        RentalUnit existing = findById(id);
        rentalUnitRepository.delete(existing);
    }

    private RentalUnitPricingDTO toPricingDTO(RentalUnit entity) {
        String referenceType;
        Long referenceId;
        String referenceCode;
        String referenceName;

        if (entity.getWarehouse() != null) {
            referenceType = "WAREHOUSE";
            referenceId = entity.getWarehouse().getId();
            referenceCode = entity.getWarehouse().getCode();
            referenceName = entity.getWarehouse().getName();
        } else if (entity.getSector() != null) {
            referenceType = "SECTOR";
            referenceId = entity.getSector().getId();
            referenceCode = entity.getSector().getCode();
            referenceName = entity.getSector().getDescription() != null
                    ? entity.getSector().getDescription()
                    : entity.getSector().getCode();
        } else if (entity.getStorageSpace() != null) {
            referenceType = "STORAGE_SPACE";
            referenceId = entity.getStorageSpace().getId();
            referenceCode = entity.getStorageSpace().getCode();
            referenceName = entity.getStorageSpace().getCode();
        } else {
            referenceType = "UNDEFINED";
            referenceId = null;
            referenceCode = null;
            referenceName = null;
        }

        return new RentalUnitPricingDTO(
                entity.getId(),
                entity.getEntityType() != null ? entity.getEntityType().getName() : null,
                referenceType,
                referenceId,
                referenceCode,
                referenceName,
                entity.getBasePrice(),
                entity.getCurrency(),
                entity.getPriceActive(),
                entity.getPriceUpdatedAt(),
                entity.getPriceUpdatedBy()
        );
    }

    private boolean isReadyForCommercialUse(RentalUnit entity) {
        return Boolean.TRUE.equals(entity.getPriceActive())
                && entity.getBasePrice() != null
                && entity.getBasePrice().compareTo(BigDecimal.ZERO) >= 0;
    }

    private void validateCurrency(String currency) {
        if (currency == null || !currency.trim().matches("^[A-Za-z]{3}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moneda invalida: use codigo ISO-4217 de 3 letras");
        }
    }

    private Long resolveActorId(String actorEmail) {
        if (actorEmail == null || actorEmail.isBlank()) {
            return null;
        }
        String normalized = EmailUtils.normalizeEmail(actorEmail);
        return appUserRepository.findByEmailIgnoreCase(normalized)
                .map(AppUser::getId)
                .orElse(null);
    }
}
