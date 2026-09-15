package ru.arslanova.orderservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Ответ с данными заказа")
public record OrderResponse (

        @Schema(example = "7f4f2d6e-1f8d-4e91-a3e5-111111111111")
        UUID id,

        @Schema(description = "Клиент, оформивший заказ", example = "00000000-0000-4000-a000-000000000004")
        UUID clientId,

        @Schema(description = "Менеджер, назначенный на заказ", example = "00000000-0000-4000-a000-000000000002")
        UUID managerId,

        @Schema(description = "Заказанный автомобиль", example = "22222222-2222-2222-2222-000000000001")
        UUID carModelId,

        @Schema(example = "IN_STOCK")
        String type,

        @Schema(example = "CREATED")
        String status,

        @Schema(description = "Комплектация: тип компонента -> деталь")
        Map<String, UUID> components
){}
