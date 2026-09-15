package ru.arslanova.storageservice.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.CarAssemblyOrder;

import java.util.UUID;

public interface CarAssemblyOrderRepository
        extends JpaRepository<CarAssemblyOrder, UUID> {
}
