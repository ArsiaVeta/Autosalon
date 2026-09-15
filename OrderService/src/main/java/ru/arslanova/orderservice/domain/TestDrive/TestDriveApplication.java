package ru.arslanova.orderservice.domain.TestDrive;

import lombok.*;
import ru.arslanova.orderservice.domain.exeptions.DomainValidationExeption;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestDriveApplication {
    private UUID id;
    public UUID clientId;
    public UUID carModelId;
    public LocalDateTime startTime;

    public TestDriveApplication(UUID clientId, UUID carModelId, LocalDateTime startTime){
        if (clientId == null){
            throw new DomainValidationExeption("Client must be provided");
        }

        if (carModelId == null){
            throw new DomainValidationExeption("Car model must be provided");
        }

        if (startTime == null || startTime.isBefore(LocalDateTime.now())){
           throw new DomainValidationExeption("Test drive start time must be in the future");
        }
        this.id = UUID.randomUUID();
        this.clientId = clientId;
        this.carModelId = carModelId;
        this.startTime = startTime;
        }
}
