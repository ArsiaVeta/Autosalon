package ru.arslanova.storageservice.api.dto;

import lombok.Getter;
import lombok.Setter;
import ru.arslanova.storageservice.domain.car.ComponentType;

import java.math.BigDecimal;

@Getter
@Setter
public class PartRequest {
    private String name;
    private ComponentType type;
    private BigDecimal price;
    private  int stockCount;
}
