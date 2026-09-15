package ru.arslanova.orderservice.domain.repository;

import java.util.Optional;
import java.util.UUID;

public interface ManagerRepository {
    Optional<UUID> findRandomManagerId();
    boolean exists(UUID managerId);
}
