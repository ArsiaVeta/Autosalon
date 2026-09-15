package ru.arslanova.orderservice;

import org.junit.jupiter.api.Test;
import ru.arslanova.orderservice.domain.TestDrive.TestDriveApplication;
import ru.arslanova.orderservice.domain.exeptions.DomainValidationExeption;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestDriveApplicationTest {
    @Test
    void shouldRejectNullClient() {
        assertThatThrownBy(() -> new TestDriveApplication(
                null, UUID.randomUUID(), LocalDateTime.now().plusDays(1)
        )).isInstanceOf(DomainValidationExeption.class);
    }

    @Test
    void shouldRejectPastStartTime() {
        assertThatThrownBy(() -> new TestDriveApplication(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().minusHours(1)
        )).isInstanceOf(DomainValidationExeption.class);
    }

    @Test
    void shouldCreateValidApplication() {
        UUID client = UUID.randomUUID();
        UUID car = UUID.randomUUID();
        LocalDateTime t = LocalDateTime.now().plusDays(2);

        TestDriveApplication app = new TestDriveApplication(client, car, t);

        assertThat(app.getId()).isNotNull();
        assertThat(app.getClientId()).isEqualTo(client);
        assertThat(app.getCarModelId()).isEqualTo(car);
        assertThat(app.getStartTime()).isEqualTo(t);
    }
}
