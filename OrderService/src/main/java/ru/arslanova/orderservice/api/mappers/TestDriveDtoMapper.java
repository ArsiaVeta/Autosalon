package ru.arslanova.orderservice.api.mappers;

import org.springframework.stereotype.Component;
import ru.arslanova.orderservice.api.dto.TestDriveResponse;
import ru.arslanova.orderservice.domain.TestDrive.TestDriveApplication;
@Component
public class TestDriveDtoMapper {
    public TestDriveResponse toResponse(TestDriveApplication app){
        return new TestDriveResponse(
                app.getId(),
                app.getClientId(),
                app.getCarModelId(),
                app.getStartTime()
        );
    }
}
