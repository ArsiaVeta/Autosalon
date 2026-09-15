package ru.arslanova.storageservice.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ru.arslanova.storageservice.infrastructure.entity.PartEntity;
import ru.arslanova.storageservice.infrastructure.repository.PartRepository;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WAREHOUSE_ADMIN', 'ADMIN')")
public class PartService {
    private final PartRepository repository;

    public PartEntity create(PartEntity part) {
        return repository.save(part);
    }

    public List<PartEntity> findAll() {
        return repository.findAll();
    }

    public PartEntity findById(UUID id) {
        return repository.findById(id).orElseThrow();
    }

    public void delete(UUID id) {repository.deleteById(id);}
}

