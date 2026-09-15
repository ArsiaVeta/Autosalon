package ru.arslanova.storageservice.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.arslanova.storageservice.infrastructure.entity.CarModelEntity;

import java.util.List;
import java.util.UUID;

public interface CarModelRepository extends
        JpaRepository<CarModelEntity, UUID>,
                JpaSpecificationExecutor<CarModelEntity> {

    List<CarModelEntity> findByStockCountGreaterThanAndRemovedFalse(int stockCount);
}
