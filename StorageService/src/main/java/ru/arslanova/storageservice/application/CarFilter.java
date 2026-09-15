package ru.arslanova.storageservice.application;

import lombok.Getter;
import lombok.Setter;
import ru.arslanova.storageservice.domain.car.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class CarFilter {
    private Brand brand;
    private String model;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BodyType bodyType;
    private FuelType fuelType;
    private GearBox gearBox;
    private DriveType driveType;
    private Color color;
    private Boolean inStockOnly;

    private Map<ComponentType, UUID> components;

}
