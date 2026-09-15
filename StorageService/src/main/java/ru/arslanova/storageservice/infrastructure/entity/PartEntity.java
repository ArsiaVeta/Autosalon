package ru.arslanova.storageservice.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.arslanova.storageservice.domain.car.ComponentType;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "parts")
public class PartEntity extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private ComponentType type;

    private BigDecimal price;

    @Column(name = "stock_count", nullable = false)
    private int stockCount = 0;

    public PartEntity(UUID id,
                      String name,
                      ComponentType type,
                      BigDecimal price,
                      int stockCount) {
        this.setId(id != null ? id : UUID.randomUUID());
        this.name = name;
        this.type = type;
        this.price = price;
        this.stockCount = stockCount;
    }
}
