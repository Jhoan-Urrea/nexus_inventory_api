package com.example.nexus.modules.sales.service;

import com.example.nexus.modules.sales.entity.Contract;
import com.example.nexus.modules.sales.entity.ContractStatus;

public interface ContractStateMachineService {

    Contract transitionStatus(Contract contract, ContractStatus newStatus);
}
