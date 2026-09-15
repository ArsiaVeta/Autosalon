package ru.arslanova.orderservice.infrastructure.mappers;

import org.springframework.stereotype.Component;
import ru.arslanova.orderservice.domain.TestDrive.TestDriveApplication;
import ru.arslanova.orderservice.infrastructure.entity.TestDriveApplicationEntity;

@Component
public class TestDrivePersistenceMapper {
    public TestDriveApplicationEntity toEntity(TestDriveApplication application) {
        return new TestDriveApplicationEntity(
                application.getId(),
                application.getClientId(),
                application.getCarModelId(),
                application.getStartTime()
        );
    }

    public TestDriveApplication toDomain(TestDriveApplicationEntity entity) {
        TestDriveApplication app = new TestDriveApplication();
        app.setId(entity.getId());
        app.setClientId(entity.getClientId());
        app.setCarModelId(entity.getCarModelId());
        app.setStartTime(entity.getStartTime());
        return app;
    }
}
