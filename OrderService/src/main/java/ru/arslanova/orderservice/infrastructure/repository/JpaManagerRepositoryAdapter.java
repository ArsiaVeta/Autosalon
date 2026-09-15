package ru.arslanova.orderservice.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.arslanova.orderservice.domain.repository.ManagerRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Repository
@RequiredArgsConstructor
public class JpaManagerRepositoryAdapter implements ManagerRepository {

    private final JpaManagerRepository jpaRepository;

    @Override
    public Optional<UUID> findRandomManagerId() {
        List<UUID> ids = jpaRepository.findActiveIds();
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ids.get(ThreadLocalRandom.current().nextInt(ids.size())));
    }

    @Override
    public boolean exists(UUID managerId) {
        return managerId != null && jpaRepository.existsByIdAndRemovedFalse(managerId);
    }
}
