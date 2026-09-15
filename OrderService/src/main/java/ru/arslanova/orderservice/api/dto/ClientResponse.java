package ru.arslanova.orderservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Ответ с данными клиента")
public record ClientResponse (
        @Schema(example = "1234567890")
        UUID id,
        @Schema(example = "Иван")
        String firstName,

        @Schema(example = "Иванов")
        String lastName,

        @Schema(example = "ivaan@ivan.com")
        String email,

        @Schema(example = "12345678910")
        String phoneNumber,

        @Schema(example = "01.01.2001")
        LocalDate dateOfBirth,

        @Schema(example = "1234567890")
        String driverLicenseNumber,

        @Schema(example = "20")
        int age
){}
