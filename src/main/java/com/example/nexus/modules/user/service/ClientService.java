package com.example.nexus.modules.user.service;

import com.example.nexus.modules.user.dto.ClientResponse;
import com.example.nexus.modules.user.dto.CreateClientRequest;
import com.example.nexus.modules.user.dto.UpdateClientRequest;

import java.util.List;

public interface ClientService {

    List<ClientResponse> findAllClients();

    ClientResponse findClientById(Long id);

    ClientResponse createClient(CreateClientRequest request);

    ClientResponse updateClient(Long id, UpdateClientRequest request);

    void deleteClient(Long id);
}
