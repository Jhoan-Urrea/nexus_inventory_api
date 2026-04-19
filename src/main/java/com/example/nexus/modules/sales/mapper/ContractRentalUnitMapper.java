package com.example.nexus.modules.sales.mapper;

import com.example.nexus.modules.sales.dto.request.CreateContractRentalUnitRequestDTO;
import com.example.nexus.modules.sales.dto.response.ContractRentalUnitResponseDTO;
import com.example.nexus.modules.sales.entity.ContractRentalUnit;
import com.example.nexus.modules.sales.entity.RentalUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ContractRentalUnitMapper {

    private final RentalUnitMapper rentalUnitMapper;

    public ContractRentalUnit toEntity(
            CreateContractRentalUnitRequestDTO dto,
            RentalUnit rentalUnit,
            BigDecimal resolvedPrice
    ) {
        return ContractRentalUnit.builder()
                .rentalUnit(rentalUnit)
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .price(resolvedPrice)
                .status(dto.status())
                .build();
    }

    public ContractRentalUnitResponseDTO toResponseDTO(ContractRentalUnit entity) {
        return new ContractRentalUnitResponseDTO(
                entity.getId(),
                entity.getContract() != null ? entity.getContract().getId() : null,
                entity.getRentalUnit() != null ? rentalUnitMapper.toResponseDTO(entity.getRentalUnit()) : null,
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPrice(),
                entity.getStatus()
        );
    }
}
