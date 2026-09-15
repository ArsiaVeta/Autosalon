package ru.arslanova.orderservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Schema(description = "DTO для создания заявки на тест-драйв ")
public class CreateTestDriveRequest {
    @Schema(example = "00000000-0000-0000-0000-000000000001")
    private UUID clientId;

    @Schema(example = "00000000-0000-0000-0000-000000000002")
    private UUID carModelId;

    @Schema(example = "2026-04-25T09:50:00")
    private LocalDateTime startTime;
}
