package ru.arslanova.orderservice.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.arslanova.orderservice.infrastructure.entity.ClientEntity;

import java.util.UUID;

public interface JpaClientRepository extends JpaRepository<ClientEntity, UUID> {
}
