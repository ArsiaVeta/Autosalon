package ru.arslanova.storageservice.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.arslanova.storageservice.infrastructure.entity.ProcessedMessageEntity;

import java.util.UUID;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessageEntity, UUID> {
    boolean existsByEventId(UUID eventID);
}
