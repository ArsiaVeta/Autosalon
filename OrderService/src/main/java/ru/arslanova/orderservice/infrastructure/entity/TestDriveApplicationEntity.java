package ru.arslanova.orderservice.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Table(name = "test_drive_applications")
public class TestDriveApplicationEntity extends BaseEntity {

    @Column(name = "client_id", nullable = false)
    public UUID clientId;

    @Column(name = "car_model_id", nullable = false)
    public UUID carModelId;

    @Column(name = "start_time", nullable = false)
    public LocalDateTime startTime;

    public TestDriveApplicationEntity(UUID id, UUID clientId, UUID carModelId,
                                      LocalDateTime startTime) {
        this.setId(id);
        this.clientId = clientId;
        this.carModelId = carModelId;
        this.startTime = startTime;
    }
}