package ru.arslanova.orderservice.api.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.arslanova.orderservice.api.dto.CarResponse;
import ru.arslanova.orderservice.infrastructure.grpc.StorageGrpcClient;

import java.util.List;

@PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
@RestController
@RequestMapping("/api/v1/cars")
@RequiredArgsConstructor
@Tag(name = "Cars", description = "Автомобили в наличии (данные из StorageService по gRPC)")
public class CarController {
    private final StorageGrpcClient storageGrpcClient;

    @GetMapping
    @Operation(summary = "Список автомобилей в наличии",
            description = "Возвращает все доступные к продаже автомобили из StorageService по gRPC",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Список получен"),
                    @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
                    @ApiResponse(responseCode = "503", description = "StorageService недоступен")
            })
    public List<CarResponse> getAll() {
        return storageGrpcClient.listAvailableCars().stream()
                .map(CarResponse::from)
                .toList();
    }
    @GetMapping("/{id}")
    @Operation(summary = "Автомобиль в наличии по идентификатору",
            description = "Возвращает конкретный доступный автомобиль из StorageService по gRPC",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Автомобиль найден"),
                    @ApiResponse(responseCode = "401", description = "Требуется аутентификация"),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
                    @ApiResponse(responseCode = "404", description = "Автомобиль не найден / не в наличии"),
                    @ApiResponse(responseCode = "503", description = "StorageService недоступен")
            })
    public CarResponse getById(@PathVariable String id) {
        return CarResponse.from(storageGrpcClient.getAvailableCar(id));
    }
}
