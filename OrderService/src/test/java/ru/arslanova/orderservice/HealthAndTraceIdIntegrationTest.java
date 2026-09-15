package ru.arslanova.orderservice;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.arslanova.orderservice.infrastructure.config.TraceIdFilter;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.topics.auto-create=false",
        "grpc.client.storage.timeout-ms=800",
        "grpc.client.storage.health-timeout-ms=800"
})
@AutoConfigureMockMvc
public class HealthAndTraceIdIntegrationTest extends AbstractPostgresIntegrationTest {

    static final String CAR_ID = "44444444-4444-4444-4444-000000000001";

    static volatile String lastSeenTraceId;


    @Autowired
    MockMvc mockMvc;

    static final int GRPC_PORT = freePort();

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
        registry.add("grpc.client.storage.port", () -> GRPC_PORT);
    }

    static Server fakeServer;

    @AfterAll
    static void shutdownServer() {
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

    static {
        try {
            fakeServer = NettyServerBuilder.forPort(GRPC_PORT)
                    .addService(new CarGrpcClientIntegrationTest.FakeStorageService())
                    .intercept(new TraceCapturingInterceptor())
                    .build()
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start fake gRPC server", e);
        }
    }

    @BeforeEach
    void resetCapturedTrace() {
        lastSeenTraceId = null;
        CarGrpcClientIntegrationTest.mode = CarGrpcClientIntegrationTest.Mode.NORMAL;
    }

    @Test
    void healthIsAvailableWithoutAuthenticationAndReportsAllDependenciesUp() throws Exception{
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("UP")))
                .andExpect(jsonPath("$.service", equalTo("order-service")))
                .andExpect(jsonPath("$.dependencies.length()", equalTo(2)))
                .andExpect(jsonPath("$.dependencies[?(@.name == 'database')].status",
                        equalTo(java.util.List.of("UP"))))
                .andExpect(jsonPath("$.dependencies[?(@.name == 'storage-service-grpc')].status",
                        equalTo(java.util.List.of("UP"))));
    }

    @Test
    void healthDoesNotBreakOtherSecurityRules() throws Exception {
        mockMvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/cars")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
    }


    @Test
    void traceIdFromClientIsReturnedInHttpResponse() throws Exception{
        String clientTrace = "trace-from-client-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/health").header(TraceIdFilter.TRACE_HEADER, clientTrace))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_HEADER, clientTrace))
                .andExpect(jsonPath("$.traceId", equalTo(clientTrace)));
    }

    @Test
    void traceIdGeneratedWhenClientDidNotSendIi() throws Exception{
        MvcResult result = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andReturn();

        String generated = result.getResponse().getHeader(TraceIdFilter.TRACE_HEADER);
        assertThat(generated).isNotBlank();
        assertThat(UUID.fromString(generated)).isNotNull();
        assertThat(result.getResponse().getContentAsString()).contains(generated);
    }

    @Test
    void traceIdFromHttpRequestReachesStorageServiceOverGrpc() throws Exception {
        String clientTrace = "trace-e2e-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/cars")
                .header(TraceIdFilter.TRACE_HEADER, clientTrace)
                .with(user("ROLE_USER")))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_HEADER, clientTrace));
        assertThat(lastSeenTraceId)
                .as("traceId из HTTP-запроса должен уйти в метаданных gRPC-вызова")
                .isEqualTo(clientTrace);
    }

    @Test
    void generatedTraceIdAlsoReachesStorageServiceOverGrpc() throws Exception{
        MvcResult result = mockMvc.perform(get("/api/v1/cars").with(user("ROLE_USER")))
                .andExpect(status().isOk())
                .andReturn();
        String generated = result.getResponse().getHeader(TraceIdFilter.TRACE_HEADER);
        assertThat(generated).isNotBlank();
        assertThat(lastSeenTraceId).isEqualTo(generated);

    }
}
