package ru.arslanova.orderservice.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.arslanova.orderservice.infrastructure.entity.TestDriveApplicationEntity;

import java.util.UUID;

public interface JpaTestDriveRepository extends JpaRepository<TestDriveApplicationEntity, UUID> {
    Page<TestDriveApplicationEntity> findAllByClientId(UUID clientId, Pageable pageable);

}
