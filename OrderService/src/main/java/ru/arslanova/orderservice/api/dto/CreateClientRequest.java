package ru.arslanova.orderservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "DTO для создания клиента")
public class CreateClientRequest {
    @Schema(example = "Иван")
    private String firstName;

    @Schema(example = "Иванов")
    private String lastName;

    @Schema(example = "ivaan@ivan.com")
    private String email;

    @Schema(example = "12345678910")
    private String phoneNumber;

    @Schema(example = "01.01.2001")
    private LocalDate dateOfBirth;

    @Schema(example = "1234567890")
    private String driverLicenseNumber;
}
