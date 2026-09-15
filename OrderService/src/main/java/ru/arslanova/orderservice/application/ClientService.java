package ru.arslanova.orderservice.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ru.arslanova.orderservice.domain.repository.ClientRepository;
import ru.arslanova.orderservice.domain.users.Client;


import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ClientService {
    private final ClientRepository repository;
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Client create(Client client){
        return repository.save(client);
    }
    @PreAuthorize("hasAnyRole('ADMIN')")
    public Client findById(UUID id){
        return repository.findById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    public Page<Client> findAll(Pageable pageable){
        return repository.findAll(pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    public void delete(UUID id){
        repository.deleteById(id);
    }

}
