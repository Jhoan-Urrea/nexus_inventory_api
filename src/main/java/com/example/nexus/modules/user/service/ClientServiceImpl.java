package com.example.nexus.modules.user.service;

import com.example.nexus.exception.BusinessException;
import com.example.nexus.exception.NotFoundException;
import com.example.nexus.exception.ValidationException;
import com.example.nexus.modules.auth.security.CurrentUserProvider;
import com.example.nexus.modules.location.entity.City;
import com.example.nexus.modules.location.repository.CityRepository;
import com.example.nexus.modules.user.dto.ClientResponse;
import com.example.nexus.modules.user.dto.CreateClientRequest;
import com.example.nexus.modules.user.dto.UpdateClientRequest;
import com.example.nexus.modules.user.entity.Client;
import com.example.nexus.modules.user.entity.ClientStatus;
import com.example.nexus.modules.user.repository.AppUserRepository;
import com.example.nexus.modules.user.repository.ClientRepository;
import com.example.nexus.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final AppUserRepository appUserRepository;
    private final CityRepository cityRepository;
    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

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
    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        String normalizedEmail = normalizeRequiredEmail(request.email());
        City city = loadCity(request.cityId());
        validateUniqueness(normalizedEmail, request.documentNumber(), null);
        validateAppUserEmailAvailability(normalizedEmail);

        Client client = Client.builder()
                .name(request.name())
                .email(normalizedEmail)
                .phone(request.phone())
                .documentType(request.documentType())
                .documentNumber(request.documentNumber())
                .businessName(request.businessName())
                .address(request.address())
                .status(ClientStatus.ACTIVE)
                .createdBy(currentUserProvider.getCurrentUserId())
                .build();

        Client savedClient = clientRepository.save(client);
        log.info("Client created clientId={}", savedClient.getId());
        userService.createPendingClientUser(savedClient, city);

        return toResponse(savedClient);
    }

    @Override
    public ClientResponse updateClient(Long id, UpdateClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        String normalizedEmail = normalizeEmail(request.email());
        validateUniqueness(normalizedEmail, request.documentNumber(), id);

        client.setName(request.name());
        client.setEmail(normalizedEmail);
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
                ? clientRepository.existsByEmailIgnoreCase(email)
                : clientRepository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
        if (emailExists) {
            throw new BusinessException("Email already in use");
        }

        boolean documentExists = currentId == null
                ? clientRepository.existsByDocumentNumber(documentNumber)
                : clientRepository.existsByDocumentNumberAndIdNot(documentNumber, currentId);
        if (documentExists) {
            throw new BusinessException("Client document number already registered");
        }
    }

    private void validateAppUserEmailAvailability(String email) {
        if (appUserRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BusinessException("Email already in use");
        }
    }

    private City loadCity(Long cityId) {
        if (cityId == null || cityId <= 0) {
            throw new ValidationException("cityId is required");
        }

        return cityRepository.findById(cityId)
                .orElseThrow(() -> new NotFoundException("City not found"));
    }

    private String normalizeRequiredEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new ValidationException("Email is required");
        }
        return normalizedEmail;
    }

    private String normalizeEmail(String email) {
        return EmailUtils.normalizeEmail(email);
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
                client.getStatus().name(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
