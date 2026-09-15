package ru.arslanova.orderservice.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.arslanova.orderservice.infrastructure.entity.OutboxEventEntity;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findAllByProcessedFalse();

    List<OutboxEventEntity> findAllByProcessedFalseAndAttemptsLessThan(int maxAttempts);
}
