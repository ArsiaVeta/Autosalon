package ru.arslanova.storageservice.api.dto;

import ru.arslanova.storageservice.domain.car.ComponentType;

import java.math.BigDecimal;
import java.util.UUID;

public record PartResponse (
        UUID id,
        String name,
        ComponentType type,
        BigDecimal price,
        int stockCount
) {}
