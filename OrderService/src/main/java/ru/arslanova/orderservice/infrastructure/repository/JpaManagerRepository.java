package ru.arslanova.orderservice.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.arslanova.orderservice.infrastructure.entity.ManagerEntity;

import java.util.List;
import java.util.UUID;

public interface JpaManagerRepository extends JpaRepository<ManagerEntity, UUID> {

    @Query("select m.id from ManagerEntity m where m.removed = false")
    List<UUID> findActiveIds();

    boolean existsByIdAndRemovedFalse(UUID id);
}
