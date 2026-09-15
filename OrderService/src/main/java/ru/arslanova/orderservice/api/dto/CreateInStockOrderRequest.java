package ru.arslanova.orderservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Schema(description = "DTO для создания заказа автомобиля со станадартной комплектацией")
public class CreateInStockOrderRequest {
    @Schema(example = "111111")
    private UUID clientId;


    @Schema(example = "222222")
    private UUID managerId;

    @Schema(example = "33333")
    private UUID carModelId;
}
