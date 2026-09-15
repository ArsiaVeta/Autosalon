package ru.arslanova.storageservice.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.arslanova.storageservice.infrastructure.entity.PartEntity;

import java.util.UUID;

public interface PartRepository extends JpaRepository<PartEntity, UUID> {

}
