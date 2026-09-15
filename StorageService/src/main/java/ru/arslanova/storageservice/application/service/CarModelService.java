package ru.arslanova.storageservice.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ru.arslanova.storageservice.application.CarFilter;
import ru.arslanova.storageservice.infrastructure.entity.CarModelEntity;
import ru.arslanova.storageservice.infrastructure.repository.CarModelRepository;
import ru.arslanova.storageservice.infrastructure.repository.CarModelSpecifications;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarModelService {
    private final CarModelRepository repository;

    @PreAuthorize("hasAnyRole('WAREHOUSE_ADMIN', 'ADMIN')")
    public CarModelEntity create(CarModelEntity car){
        return repository.save(car);
    }

    @PreAuthorize("hasAnyRole('WAREHOUSE_ADMIN', 'ADMIN')")
    public Page<CarModelEntity> findAll(CarFilter filter, Pageable pageable) {
        return repository.findAll(CarModelSpecifications.withFilter(filter), pageable);
    }

    @PreAuthorize("hasAnyRole('WAREHOUSE_ADMIN', 'ADMIN')")
    public CarModelEntity findById(UUID id) { return repository.findById(id).orElseThrow(); }

    @PreAuthorize("hasAnyRole('WAREHOUSE_ADMIN', 'ADMIN')")
    public void delete(UUID id) { repository.deleteById(id); }
}
