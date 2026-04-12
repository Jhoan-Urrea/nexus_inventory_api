package com.example.nexus.modules.sales.mapper;

import com.example.nexus.modules.sales.dto.request.CreateContractRequest;
import com.example.nexus.modules.sales.dto.response.ContractResponseDTO;
import com.example.nexus.modules.sales.dto.response.ContractRentalUnitResponseDTO;
import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractStatus;
import com.example.nexus.modules.user.entity.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ContractMapper {

    private final ContractRentalUnitMapper contractRentalUnitMapper;

    public Contract toEntity(CreateContractRequest request, Client client) {
        return Contract.builder()
                .client(client)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .totalAmount(BigDecimal.ZERO)
                .status(ContractStatus.DRAFT.getCode())
                .build();
    }

    public ContractResponseDTO toResponseDTO(Contract entity) {
        ContractStatus status = ContractStatus.fromCode(entity.getStatus());
        List<ContractRentalUnitResponseDTO> contractRentalUnits = entity.getRentalUnits() == null
                ? List.of()
                : entity.getRentalUnits().stream()
                .sorted(Comparator.comparing(com.example.nexus.modules.sales.entity.ContractRentalUnit::getStartDate)
                        .thenComparing(com.example.nexus.modules.sales.entity.ContractRentalUnit::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(contractRentalUnitMapper::toResponseDTO)
                .toList();

        return new ContractResponseDTO(
                entity.getId(),
                entity.getClient() != null ? entity.getClient().getId() : null,
                entity.getClient() != null ? entity.getClient().getName() : null,
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getTotalAmount(),
                entity.getStatus(),
                status != null ? status.name() : null,
                contractRentalUnits
        );
    }
}
