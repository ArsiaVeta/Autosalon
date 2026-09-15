package ru.arslanova.orderservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "DTO для создания заказа автомобиля с кастомной комплектацией")
public class CreateCustomOrderRequest {
    @Schema(example = "111111")
    private UUID clientId;

    @Schema(example = "222222")
    private UUID managerId;

    @Schema(example = "33333")
    private UUID carModelId;

    @Schema(description = "Комплектующие автомобиля. Ключ - тип компонента, значение - UUID детали")
    private Map<String, UUID> components;
}
