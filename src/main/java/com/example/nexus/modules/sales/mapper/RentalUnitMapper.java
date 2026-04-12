package com.example.nexus.modules.sales.mapper;

import com.example.nexus.modules.sales.dto.response.RentalUnitDTO;
import com.example.nexus.modules.sales.dto.response.RentalUnitResponseDTO;
import com.example.nexus.modules.sales.entity.RentalUnit;
import org.springframework.stereotype.Component;

@Component
public class RentalUnitMapper {

    public RentalUnitResponseDTO toResponseDTO(RentalUnit entity) {
        String referenceType = resolveReferenceType(entity);
        Long referenceId = resolveReferenceId(entity);
        String referenceCode = resolveReferenceCode(entity);
        String referenceName = resolveReferenceName(entity);

        return new RentalUnitResponseDTO(
                entity.getId(),
                entity.getEntityType() != null ? entity.getEntityType().getId() : null,
                entity.getEntityType() != null ? entity.getEntityType().getName() : null,
                referenceType,
                referenceId,
                referenceCode,
                referenceName,
                buildDisplayName(referenceType, referenceCode, referenceName, entity.getId())
        );
    }

    public RentalUnitDTO toDto(RentalUnit entity) {
        return new RentalUnitDTO(
                entity.getId(),
                entity.getWarehouse() != null ? entity.getWarehouse().getId() : null,
                entity.getSector() != null ? entity.getSector().getId() : null,
                entity.getStorageSpace() != null ? entity.getStorageSpace().getId() : null,
                entity.getEntityType() != null ? entity.getEntityType().getId() : null
        );
    }

    private String resolveReferenceType(RentalUnit entity) {
        if (entity.getWarehouse() != null) {
            return "WAREHOUSE";
        }
        if (entity.getSector() != null) {
            return "SECTOR";
        }
        if (entity.getStorageSpace() != null) {
            return "STORAGE_SPACE";
        }
        return "UNDEFINED";
    }

    private Long resolveReferenceId(RentalUnit entity) {
        if (entity.getWarehouse() != null) {
            return entity.getWarehouse().getId();
        }
        if (entity.getSector() != null) {
            return entity.getSector().getId();
        }
        if (entity.getStorageSpace() != null) {
            return entity.getStorageSpace().getId();
        }
        return null;
    }

    private String resolveReferenceCode(RentalUnit entity) {
        if (entity.getWarehouse() != null) {
            return entity.getWarehouse().getCode();
        }
        if (entity.getSector() != null) {
            return entity.getSector().getCode();
        }
        if (entity.getStorageSpace() != null) {
            return entity.getStorageSpace().getCode();
        }
        return null;
    }

    private String resolveReferenceName(RentalUnit entity) {
        if (entity.getWarehouse() != null) {
            return entity.getWarehouse().getName();
        }
        if (entity.getSector() != null) {
            return entity.getSector().getDescription() != null
                    ? entity.getSector().getDescription()
                    : entity.getSector().getCode();
        }
        if (entity.getStorageSpace() != null) {
            return entity.getStorageSpace().getCode();
        }
        return null;
    }

    private String buildDisplayName(String referenceType, String referenceCode, String referenceName, Long rentalUnitId) {
        if (referenceCode != null && referenceName != null && !referenceName.equals(referenceCode)) {
            return referenceType + " - " + referenceCode + " - " + referenceName;
        }
        if (referenceCode != null) {
            return referenceType + " - " + referenceCode;
        }
        if (referenceName != null) {
            return referenceType + " - " + referenceName;
        }
        return referenceType + " - " + rentalUnitId;
    }
}
