package ru.arslanova.orderservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Ответ с данными заявки на тестдрайв")
public record TestDriveResponse (
        @Schema(example = "7f4f2d6e-1f8d-4e91-a3e5-444444444444")
        UUID id,

        @Schema(example = "7f4f2d6e-1f8d-4e91-a3e5-111111111111")
        UUID clientId,

        @Schema(example = "7f4f2d6e-1f8d-4e91-a3e5-333333333333")
        UUID carModelId,

        @Schema(example = "2026-04-25T09:50:00")
        LocalDateTime startTime
){}
