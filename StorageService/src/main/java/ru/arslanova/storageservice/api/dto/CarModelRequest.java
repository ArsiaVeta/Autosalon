package ru.arslanova.storageservice.api.dto;

import lombok.Getter;
import lombok.Setter;
import ru.arslanova.storageservice.domain.car.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class CarModelRequest {
    private Brand brand;
    private String model;
    private BigDecimal basePrice;
    private BodyType bodyType;
    private FuelType fuelType;
    private DriveType driveType;
    private GearBox gearBox;
    private Color color;
    private int stockCount;
    private Map<ComponentType, UUID> baseComponents;
}
