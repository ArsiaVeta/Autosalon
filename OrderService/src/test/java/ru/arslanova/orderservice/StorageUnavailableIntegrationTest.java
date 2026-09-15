package ru.arslanova.orderservice;

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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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
        "grpc.client.storage.timeout-ms=800",
        "grpc.client.storage.health-timeout-ms=800"
})
@AutoConfigureMockMvc
public class StorageUnavailableIntegrationTest extends AbstractPostgresIntegrationTest {
    static final int DEAD_PORT = closedPort();

    private static int closedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void grpcProps(DynamicPropertyRegistry registry) {
        registry.add("grpc.client.storage.host", () -> "localhost");
        registry.add("grpc.client.storage.port", () -> DEAD_PORT);
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

    @Test
    void listCarsReturn503WhenStorageIsDown() throws Exception {
        mockMvc.perform(get("/api/v1/cars").with(user("ROLE_USER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", equalTo(503)))
                .andExpect(jsonPath("$.error", equalTo("SERVICE_UNAVAILABLE")));
    }

    @Test
    void getCarByIdReturn503WhenStorageIsDown() throws Exception {
        mockMvc.perform(get("/api/v1/cars/" + UUID.randomUUID()).with(user("ROLE_USER")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", equalTo(503)))
                .andExpect(jsonPath("$.error", equalTo("SERVICE_UNAVAILABLE")));
    }

    @Test
    void healthReports503AndMarksStorageDown() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", equalTo("DOWN")))
                .andExpect(jsonPath("$.service", equalTo("order-service")))
                .andExpect(jsonPath("$.dependencies[?(@.name == 'database')].status",
                        equalTo(java.util.List.of("UP"))))
                .andExpect(jsonPath("$.dependencies[?(@.name == 'storage-service-grpc')].status",
                        equalTo(java.util.List.of("DOWN"))));
    }

    private static RequestPostProcessor user(String role) {
        return jwt().jwt(b -> b.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority(role));
    }
}