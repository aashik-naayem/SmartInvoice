package com.smartinvoice.service;

import com.smartinvoice.dto.ClientRequest;
import com.smartinvoice.dto.ClientResponse;
import com.smartinvoice.entity.Client;
import com.smartinvoice.entity.User;
import com.smartinvoice.exception.ResourceNotFoundException;
import com.smartinvoice.repository.ClientRepository;
import com.smartinvoice.repository.UserRepository;
import com.smartinvoice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClientResponse create(ClientRequest request) {
        User currentUser = getCurrentUser();

        Client client = Client.builder()
                .user(currentUser)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        return toResponse(clientRepository.save(client));
    }

    public List<ClientResponse> getAll() {
        User currentUser = getCurrentUser();
        return clientRepository.findByUser(currentUser).stream()
                .map(this::toResponse)
                .toList();
    }

    public ClientResponse getById(Long id) {
        return toResponse(findOwnedClient(id));
    }

    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = findOwnedClient(id);
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setAddress(request.getAddress());
        return toResponse(clientRepository.save(client));
    }

    @Transactional
    public void delete(Long id) {
        Client client = findOwnedClient(id);
        clientRepository.delete(client);
    }

    private Client findOwnedClient(Long id) {
        User currentUser = getCurrentUser();
        return clientRepository.findByIdAndUser(id, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found with id: " + id));
    }

    private User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private ClientResponse toResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .address(client.getAddress())
                .createdAt(client.getCreatedAt())
                .build();
    }
}
