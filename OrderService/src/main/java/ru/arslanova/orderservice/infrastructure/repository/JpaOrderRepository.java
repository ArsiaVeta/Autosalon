package ru.arslanova.orderservice.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.arslanova.orderservice.infrastructure.entity.OrderEntity;

import java.util.UUID;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, UUID> {
    Page<OrderEntity> findAllByClientId(UUID clientId, Pageable pageable);
}
