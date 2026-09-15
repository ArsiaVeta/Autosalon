package ru.arslanova.storageservice.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.arslanova.storageservice.domain.car.*;

import java.util.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Table(name = "car_models")
public class CarModelEntity extends BaseEntity{

    @Enumerated(EnumType.STRING)
    private Brand brand;

    @Column(nullable = false)
    private String model;

    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    private BodyType bodyType;

    @Enumerated(EnumType.STRING)
    private FuelType fuelType;

    @Enumerated(EnumType.STRING)
    private DriveType driveType;

    @Enumerated(EnumType.STRING)
    private GearBox gearBox;

    @Enumerated(EnumType.STRING)
    private Color color;

    @Column(name = "stock_count", nullable = false)
    private int stockCount = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "car_model_components",
            joinColumns = @JoinColumn(name = "car_model_id")
    )
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "component_type")
    @Column(name = "part_id")
    private Map<ComponentType, UUID> baseComponents = new EnumMap<>(ComponentType.class);


    public CarModelEntity(UUID id, Brand brand, String model, BigDecimal basePrice,
                          BodyType bodyType, FuelType fuelType, DriveType driveType,
                          GearBox gearBox, Color color, int stockCount,
                          Map<ComponentType, UUID> baseComponents) {
        this.setId(id != null ? id : UUID.randomUUID());
        this.brand = brand;
        this.model = model;
        this.basePrice = basePrice;
        this.bodyType = bodyType;
        this.fuelType = fuelType;
        this.driveType = driveType;
        this.gearBox = gearBox;
        this.color = color;
        this.stockCount = stockCount;
        if (baseComponents != null) {
            this.baseComponents = new EnumMap<>(baseComponents);
        }
    }

    public CarModelEntity(UUID id, Brand brand, String model, BigDecimal basePrice,
                          BodyType bodyType, FuelType fuelType, DriveType driveType,
                          GearBox gearBox, Color color, int stockCount) {
        this(id, brand, model, basePrice, bodyType, fuelType, driveType, gearBox, color, stockCount, null);
    }
}
