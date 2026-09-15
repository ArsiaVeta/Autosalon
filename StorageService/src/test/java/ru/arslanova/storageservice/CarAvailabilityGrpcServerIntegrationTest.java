package ru.arslanova.storageservice;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import ru.arslanova.grpc.car.*;
import ru.arslanova.storageservice.domain.car.*;
import ru.arslanova.storageservice.infrastructure.entity.CarModelEntity;
import ru.arslanova.storageservice.infrastructure.grpc.CarAvailabilityGrpcService;
import ru.arslanova.storageservice.infrastructure.repository.CarModelRepository;

import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.topics.auto-create=false"
})
public class CarAvailabilityGrpcServerIntegrationTest extends AbstractPostgresIntegrationTest{

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public JwtDecoder jwtDecoder() {
            byte[] key = "test-secret-test-secret-test-secret-key".getBytes();
            return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256")).build();
        }
    }

    @Autowired
    CarAvailabilityGrpcService grpcService;
    @Autowired
    CarModelRepository repository;

    private Server server;
    private ManagedChannel channel;
    private CarAvailabilityServiceGrpc.CarAvailabilityServiceBlockingStub stub;

    @BeforeEach
    void setUp() throws Exception {
        repository.deleteAll();
        server = NettyServerBuilder.forPort(0)
                .addService(grpcService)
                .build()
                .start();
        channel = NettyChannelBuilder.forAddress("localhost", server.getPort())
                .usePlaintext()
                .build();
        stub = CarAvailabilityServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (channel != null) channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        if (server != null) server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        repository.deleteAll();
    }

    private CarModelEntity car(String model, int stock) {
        return new CarModelEntity(
                UUID.randomUUID(), Brand.BMW, model, new BigDecimal("3500000"),
                BodyType.SEDAN, FuelType.PETROL, DriveType.REAR,
                GearBox.AUTOMATIC, Color.BLACK, stock);
    }

    @Test
    void listAvailableCarsReturnsOnlyInStock() {
        repository.save(car("320i", 2));
        repository.save(car("520d", 1));
        repository.save(car("M3", 0));

        ListAvailableCarsResponse response =
                stub.listAvailableCars(ListAvailableCarsRequest.getDefaultInstance());

        assertThat(response.getCarsList())
                .extracting(AvailableCar::getModel)
                .containsExactlyInAnyOrder("320i", "520d");
        assertThat(response.getCarsList())
                .allMatch(c -> c.getStockCount() > 0);
    }

    @Test
    void listAvailableCarsReturnsEmptyWhenNothingInStock() {
        repository.save(car("M3", 0));

        ListAvailableCarsResponse response =
                stub.listAvailableCars(ListAvailableCarsRequest.getDefaultInstance());

        assertThat(response.getCarsList()).isEmpty();
    }

    @Test
    void getAvailableCarReturnsInStockCar() {
        CarModelEntity saved = repository.save(car("320i", 2));

        AvailableCar car = stub.getAvailableCar(
                GetAvailableCarRequest.newBuilder().setId(saved.getId().toString()).build());

        assertThat(car.getId()).isEqualTo(saved.getId().toString());
        assertThat(car.getModel()).isEqualTo("320i");
        assertThat(car.getBrand()).isEqualTo("BMW");
        assertThat(car.getStockCount()).isEqualTo(2);
    }

    @Test
    void getAvailableCarForOutOfStockReturnsNotFound() {
        CarModelEntity saved = repository.save(car("M3", 0));

        assertThatThrownBy(() -> stub.getAvailableCar(
                GetAvailableCarRequest.newBuilder().setId(saved.getId().toString()).build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("NOT_FOUND");
    }

    @Test
    void getAvailableCarForMissingIdReturnsNotFound() {
        assertThatThrownBy(() -> stub.getAvailableCar(
                GetAvailableCarRequest.newBuilder().setId(UUID.randomUUID().toString()).build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("NOT_FOUND");
    }
}
