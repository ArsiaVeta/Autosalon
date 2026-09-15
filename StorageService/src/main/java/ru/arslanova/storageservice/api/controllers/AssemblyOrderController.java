package ru.arslanova.storageservice.api.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.arslanova.storageservice.api.dto.AssemblyOrderResponse;
import ru.arslanova.storageservice.api.dto.CreateAssemblyOrderRequest;import ru.arslanova.storageservice.api.mappers.AssemblyOrderMapper;
import ru.arslanova.storageservice.application.service.AssemblyOrderService;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.AssemblyState;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/assembly-orders")
public class AssemblyOrderController {
    private final AssemblyOrderService service;
    private final AssemblyOrderMapper mapper;

    @PostMapping
    @PreAuthorize("""
        hasRole('WAREHOUSE_ADMIN')
        or hasRole('ADMIN')
    """)
    public AssemblyOrderResponse create(
            @RequestBody CreateAssemblyOrderRequest request){
        return mapper.toResponse(service.create(request.getSourceOrderId()));
    }

    @GetMapping
    @PreAuthorize("""
        hasRole('WAREHOUSE_ADMIN')
        or hasRole('ADMIN')
    """)
    public List<AssemblyOrderResponse> findAll() {

        return service.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("""
        hasRole('WAREHOUSE_ADMIN')
        or hasRole('ADMIN')
    """)
    public AssemblyOrderResponse findById(
            @PathVariable UUID id
    ) {

        return mapper.toResponse(
                service.findById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("""
        hasRole('WAREHOUSE_ADMIN')
        or hasRole('ADMIN')
    """)
    public AssemblyOrderResponse update(
            @PathVariable UUID id,
            @RequestParam AssemblyState state
    ) {

        return mapper.toResponse(
                service.updateState(id, state)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("""
        hasRole('WAREHOUSE_ADMIN')
        or hasRole('ADMIN')
    """)
    public void delete(
            @PathVariable UUID id
    ) {

        service.delete(id);
    }
}
