package ru.arslanova.orderservice;

import io.grpc.Server;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ru.arslanova.grpc.car.*;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.topics.auto-create=false",
        "grpc.client.storage.timeout-ms=800"
})
@AutoConfigureMockMvc
public class CarGrpcClientIntegrationTest extends AbstractPostgresIntegrationTest{
    enum Mode { NORMAL, EMPTY, UNAVAILABLE, SLOW }

    static final String CAR_ID = "33333333-3333-3333-3333-000000000001";
    static volatile Mode mode = Mode.NORMAL;
    static int grpcPort;
    static Server fakeServer;

    static {
        grpcPort = freePort();
        try {
            fakeServer = NettyServerBuilder.forPort(grpcPort)
                    .addService(new FakeStorageService())
                    .build()
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start fake gRPC server", e);
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void grpcProps(DynamicPropertyRegistry registry) {
        registry.add("grpc.client.storage.host", () -> "localhost");
        registry.add("grpc.client.storage.port", () -> grpcPort);
    }

    @AfterAll
    static void shutdownServer() throws Exception {
        if (fakeServer != null) {
            fakeServer.shutdownNow();
        }
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public JwtDecoder jwtDecoder() {
            byte[] key = "test-secret-test-secret-test-secret-key".getBytes();
            return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256")).build();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void resetMode() {
        mode = Mode.NORMAL;
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(String role) {
        return jwt().jwt(b -> b.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority(role));
    }

    @Test
    void getAllReturnsCarsForUser() throws Exception {
        mode = Mode.NORMAL;
        mockMvc.perform(get("/api/v1/cars").with(user("ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", equalTo(1)))
                .andExpect(jsonPath("$[0].model", equalTo("320i")))
                .andExpect(jsonPath("$[0].brand", equalTo("BMW")));
    }
    @Test
    void getAllReturnsEmptyList() throws Exception {
        mode = Mode.EMPTY;
        mockMvc.perform(get("/api/v1/cars").with(user("ROLE_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", equalTo(0)));
    }

    @Test
    void getAllReturns503WhenStorageUnavailable() throws Exception {
        mode = Mode.UNAVAILABLE;
        mockMvc.perform(get("/api/v1/cars").with(user("ROLE_USER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", equalTo(503)))
                .andExpect(jsonPath("$.error", equalTo("SERVICE_UNAVAILABLE")));
    }

    @Test
    void getAllReturns503OnTimeout() throws Exception {
        mode = Mode.SLOW;
        mockMvc.perform(get("/api/v1/cars").with(user("ROLE_USER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", equalTo(503)))
                .andExpect(jsonPath("$.error", equalTo("SERVICE_UNAVAILABLE")));
    }

    @Test
    void getByIdReturnsCar() throws Exception {
        mode = Mode.NORMAL;
        mockMvc.perform(get("/api/v1/cars/" + CAR_ID).with(user("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(CAR_ID)))
                .andExpect(jsonPath("$.model", equalTo("320i")));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        mode = Mode.NORMAL;
        mockMvc.perform(get("/api/v1/cars/" + UUID.randomUUID()).with(user("ROLE_USER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", equalTo(404)))
                .andExpect(jsonPath("$.error", equalTo("NOT_FOUND")));
    }

    @Test
    void anonymousRequestIs401() throws Exception {
        mockMvc.perform(get("/api/v1/cars"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", equalTo("UNAUTHORIZED")));
    }

    @Test
    void forbiddenRoleIs403() throws Exception {
        mockMvc.perform(get("/api/v1/cars").with(user("ROLE_WAREHOUSE_ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", equalTo("FORBIDDEN")));
    }

    static class FakeStorageService
            extends CarAvailabilityServiceGrpc.CarAvailabilityServiceImplBase {

        @Override
        public void listAvailableCars(ListAvailableCarsRequest request,
                                      StreamObserver<ListAvailableCarsResponse> obs) {
            if (mode == Mode.UNAVAILABLE) {
                obs.onError(Status.UNAVAILABLE.withDescription("storage down").asRuntimeException());
                return;
            }
            if (mode == Mode.SLOW) {
                sleep();
            }
            if (mode == Mode.EMPTY) {
                obs.onNext(ListAvailableCarsResponse.getDefaultInstance());
            } else {
                obs.onNext(ListAvailableCarsResponse.newBuilder().addCars(sampleCar()).build());
            }
            obs.onCompleted();
        }

        @Override
        public void getAvailableCar(GetAvailableCarRequest request,
                                    StreamObserver<AvailableCar> obs) {
            if (mode == Mode.UNAVAILABLE) {
                obs.onError(Status.UNAVAILABLE.withDescription("storage down").asRuntimeException());
                return;
            }
            if (mode == Mode.SLOW) {
                sleep();
            }
            if (CAR_ID.equals(request.getId())) {
                obs.onNext(sampleCar());
                obs.onCompleted();
            } else {
                obs.onError(Status.NOT_FOUND
                        .withDescription("Available car not found: " + request.getId())
                        .asRuntimeException());
            }
        }

        private static void sleep() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private static AvailableCar sampleCar() {
            return AvailableCar.newBuilder()
                    .setId(CAR_ID)
                    .setBrand("BMW")
                    .setModel("320i")
                    .setBasePrice("3500000")
                    .setBodyType("SEDAN")
                    .setFuelType("PETROL")
                    .setDriveType("REAR")
                    .setGearBox("AUTOMATIC")
                    .setColor("BLACK")
                    .setStockCount(2)
                    .build();
        }
    }

}
