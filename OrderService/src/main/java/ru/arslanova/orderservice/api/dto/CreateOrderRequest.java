package ru.arslanova.orderservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "DTO для создания заказа автомобиля")
public class CreateOrderRequest {
    @Schema(example = "111111")
    private UUID clientId;

    @Schema(example = "IN_STOCK")
    private String type;

    @Schema(example = "00000000-0000-4000-8000-000000000001")
    private UUID carModelId;

    @Schema(description = "Комплектующие автомобиля. Ключ - тип компонента, значение - UUID детали")
    private Map<String, UUID> components;
}
