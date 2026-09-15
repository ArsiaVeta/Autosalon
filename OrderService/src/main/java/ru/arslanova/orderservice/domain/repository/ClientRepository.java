package ru.arslanova.orderservice.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.arslanova.orderservice.domain.users.Client;


import java.util.UUID;

public interface ClientRepository {
    Client findById(UUID id);
    Page<Client> findAll(Pageable pageable);
    Client save(Client client);
    void deleteById(UUID id);
}
