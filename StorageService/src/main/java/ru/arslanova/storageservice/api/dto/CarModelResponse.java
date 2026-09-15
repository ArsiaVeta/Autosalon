package ru.arslanova.storageservice.api.dto;

import ru.arslanova.storageservice.domain.car.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CarModelResponse (
    UUID id,
    Brand brand,
    String model,
    BigDecimal basePrice,
    BodyType bodyType,
    FuelType fuelType,
    DriveType driveType,
    GearBox gearBox,
    Color color,
    int stockCount
) {}
