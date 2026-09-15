package ru.arslanova.storageservice.api.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.arslanova.storageservice.api.dto.CarModelRequest;
import ru.arslanova.storageservice.api.dto.CarModelResponse;
import ru.arslanova.storageservice.application.CarFilter;
import ru.arslanova.storageservice.application.service.CarModelService;
import ru.arslanova.storageservice.domain.car.*;
import ru.arslanova.storageservice.infrastructure.entity.CarModelEntity;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@PreAuthorize("hasAnyRole('WAREHOUSE_ADMIN', 'ADMIN')")
@RestController
@RequestMapping("/api/car-models")
@RequiredArgsConstructor
@Tag(name = "CarModels", description = "Работа с автомобилями")
public class CarModelController {

    private static final String COMPONENT_PARAM_PREFIX = "component[";

    private final CarModelService service;

    @PostMapping
    public CarModelResponse create(@RequestBody CarModelRequest request){
        CarModelEntity entity = new CarModelEntity(
                null,
                request.getBrand(),
                request.getModel(),
                request.getBasePrice(),
                request.getBodyType(),
                request.getFuelType(),
                request.getDriveType(),
                request.getGearBox(),
                request.getColor(),
                request.getStockCount(),
                request.getBaseComponents()
        );
        return toResponse(service.create(entity));
    }

    @GetMapping
    public Page<CarModelResponse> findAll(
            @Parameter(description = "Бренд автомобиля") @RequestParam(required = false) Brand brand,
            @Parameter(description = "Модель (только при выбранном бренде)") @RequestParam(required = false) String model,
            @Parameter(description = "Мин. цена") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Макс. цена") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Тип кузова") @RequestParam(required = false) BodyType bodyType,
            @Parameter(description = "Тип топлива") @RequestParam(required = false) FuelType fuelType,
            @Parameter(description = "КПП") @RequestParam(required = false) GearBox gearBox,
            @Parameter(description = "Привод") @RequestParam(required = false) DriveType driveType,
            @Parameter(description = "Цвет") @RequestParam(required = false) Color color,
            @Parameter(description = "Только в наличии") @RequestParam(required = false) Boolean inStockOnly,
            @Parameter(description = "Фильтр по комплектующим. Пример: component[WHEEL]=<uuid>")
            @RequestParam(required = false) Map<String, String> allParams,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (model != null && !model.isBlank() && brand == null) {
            throw new IllegalArgumentException("Filter by model is only allowed when brand is selected");
        }
        CarFilter filter = new CarFilter();
        filter.setBrand(brand);
        filter.setModel(model);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setBodyType(bodyType);
        filter.setFuelType(fuelType);
        filter.setGearBox(gearBox);
        filter.setDriveType(driveType);
        filter.setColor(color);
        filter.setInStockOnly(inStockOnly);
        Map<ComponentType, UUID> components = extractComponents(allParams);
        if (!components.isEmpty()) {
            filter.setComponents(components);
        }
        return service.findAll(filter, pageable).map(this::toResponse);
    }
    @GetMapping("/{id}")
    public CarModelResponse findById(@PathVariable UUID id) {
        return toResponse(service.findById(id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    private Map<ComponentType, UUID> extractComponents(Map<String, String> params) {
        Map<ComponentType, UUID> components = new EnumMap<>(ComponentType.class);
        if (params == null) {
            return components;
        }
        params.forEach((key, value) -> {
            if (key.startsWith(COMPONENT_PARAM_PREFIX) && key.endsWith("]")) {
                String type = key.substring(COMPONENT_PARAM_PREFIX.length(), key.length() - 1);
                components.put(ComponentType.valueOf(type), UUID.fromString(value));
            }
        });
        return components;
    }

    private CarModelResponse toResponse(CarModelEntity e){
        return new CarModelResponse(
                e.getId(), e.getBrand(), e.getModel(), e.getBasePrice(),
                e.getBodyType(), e.getFuelType(), e.getDriveType(),
                e.getGearBox(), e.getColor(), e.getStockCount()
        );
    }
}
