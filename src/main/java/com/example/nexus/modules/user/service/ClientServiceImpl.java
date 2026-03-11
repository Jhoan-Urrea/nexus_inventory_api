package com.example.nexus.modules.user.service;

import com.example.nexus.modules.user.dto.ClientResponse;
import com.example.nexus.modules.user.dto.CreateClientRequest;
import com.example.nexus.modules.user.dto.UpdateClientRequest;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.ClientStatus;
import com.example.nexus.modules.user.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    public List<ClientResponse> findAllClients() {
        return clientRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ClientResponse findClientById(Long id) {
        return clientRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    @Override
    public ClientResponse createClient(CreateClientRequest request) {
        validateUniqueness(request.email(), request.documentNumber(), null);

        Client client = Client.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .businessName(request.businessName())
                .address(request.address())
                .status(ClientStatus.ACTIVE)
                .build();

        return toResponse(clientRepository.save(client));
    }

    @Override
    public ClientResponse updateClient(Long id, UpdateClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        validateUniqueness(request.email(), request.documentNumber(), id);

        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setDocumentType(request.documentType());
        client.setDocumentNumber(request.documentNumber());
        client.setBusinessName(request.businessName());
        client.setAddress(request.address());
        if (request.status() != null) {
            client.setStatus(request.status());
        }

        return toResponse(clientRepository.save(client));
    }

    @Override
    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
        clientRepository.delete(client);
    }

    private void validateUniqueness(String email, String documentNumber, Long currentId) {
        boolean emailExists = currentId == null
                ? clientRepository.existsByEmail(email)
                : clientRepository.existsByEmailAndIdNot(email, currentId);
        if (emailExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Client email already registered");
        }

        boolean documentExists = currentId == null
                ? clientRepository.existsByDocumentNumber(documentNumber)
                : clientRepository.existsByDocumentNumberAndIdNot(documentNumber, currentId);
        if (documentExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Client document number already registered");
        }
    }

    private ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getDocumentType(),
                client.getDocumentNumber(),
                client.getBusinessName(),
                client.getAddress(),
                client.getStatus().name()
        );
    }
}
