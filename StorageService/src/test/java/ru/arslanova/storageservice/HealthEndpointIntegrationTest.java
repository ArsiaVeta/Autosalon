package ru.arslanova.storageservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.arslanova.storageservice.infrastructure.config.TraceIdFilter;

import javax.crypto.spec.SecretKeySpec;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.topics.auto-create=false"
})
@AutoConfigureMockMvc
public class HealthEndpointIntegrationTest extends AbstractPostgresIntegrationTest {

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
    void healthIsAvailableWithoutAuthentication() throws Exception{
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("UP")))
                .andExpect(jsonPath("$.service", equalTo("storage-service")))
                .andExpect(jsonPath("$.dependencies.length()", equalTo(1)))
                .andExpect(jsonPath("$.dependencies[?(@.name == 'database')].status",
                        equalTo(List.of("UP"))));
    }

    @Test
    void otherEndpointsStillRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/car-models")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/parts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/assembly-orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthResultTracedFromRequest() throws Exception {
        String clientTrace = "trace-from-client-" + UUID.randomUUID();

        mockMvc.perform(get("/api/v1/health").header(TraceIdFilter.TRACE_HEADER, clientTrace))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_HEADER, clientTrace))
                .andExpect(jsonPath("$.traceId", equalTo(clientTrace)));
    }

    @Test
    void healthGeneratesTraceIdWhenAbsent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andReturn();

        String generated = result.getResponse().getHeader(TraceIdFilter.TRACE_HEADER);
        assertThat(generated).isNotBlank();
        assertThat(UUID.fromString(generated)).isNotNull();
        assertThat(result.getResponse().getContentAsString()).contains(generated);
    }
}
