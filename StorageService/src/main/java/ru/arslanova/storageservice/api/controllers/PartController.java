package ru.arslanova.storageservice.api.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.arslanova.storageservice.api.dto.PartRequest;
import ru.arslanova.storageservice.api.dto.PartResponse;
import ru.arslanova.storageservice.application.service.PartService;
import ru.arslanova.storageservice.infrastructure.entity.PartEntity;

import java.util.List;
import java.util.UUID;


@PreAuthorize("hasAnyRole('WAREHOUSE_ADMIN', 'ADMIN')")
@RestController
@RequestMapping("/api/parts")
@RequiredArgsConstructor
@Tag(name = "Parts", description = "Работа с запчастями")

public class PartController {
    private final PartService service;

    @PostMapping
    public PartResponse create(@RequestBody PartRequest request) {
        PartEntity entity = new PartEntity(
                null,
                request.getName(),
                request.getType(),
                request.getPrice(),
                request.getStockCount()
        );

        return toResponse(service.create(entity));
    }

    @GetMapping
    public List<PartResponse> findAll() {
        return service.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PartResponse findById(@PathVariable UUID id) {
        return toResponse(service.findById(id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private PartResponse toResponse(PartEntity e){
        return new PartResponse(e.getId(), e.getName(), e.getType(), e.getPrice(), e.getStockCount());
    }
}