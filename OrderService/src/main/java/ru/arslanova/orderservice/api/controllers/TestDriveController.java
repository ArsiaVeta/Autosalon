package ru.arslanova.orderservice.api.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.arslanova.orderservice.api.dto.CreateTestDriveRequest;
import ru.arslanova.orderservice.api.dto.TestDriveResponse;
import ru.arslanova.orderservice.api.mappers.TestDriveDtoMapper;
import ru.arslanova.orderservice.application.TestDriveService;

import java.util.UUID;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api/test_drives")
@RequiredArgsConstructor
@Tag(name = "TestDrive", description = "Работа с заявками на тест-драйв")

public class TestDriveController {

    private final TestDriveService service;
    private final TestDriveDtoMapper mapper;

    @PostMapping
    public TestDriveResponse create(@RequestBody CreateTestDriveRequest request){
        return mapper.toResponse(service.create(
                        request.getClientId(),
                        request.getCarModelId(),
                        request.getStartTime()
        ));
    }

    @GetMapping
    public Page<TestDriveResponse> getAll(@PageableDefault(size = 20) Pageable pageable){
        return service.findMine(pageable).map(mapper::toResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
