package ru.arslanova.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.arslanova.orderservice.domain.Order.OrderType;
import ru.arslanova.orderservice.api.dto.CreateOrderRequest;
import ru.arslanova.orderservice.infrastructure.entity.ManagerEntity;
import ru.arslanova.orderservice.infrastructure.repository.JpaManagerRepository;

import javax.crypto.spec.SecretKeySpec;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.topics.auto-create=false"
})
@AutoConfigureMockMvc
@org.springframework.transaction.annotation.Transactional
class OrderControllerSecurityIntegrationTest extends AbstractPostgresIntegrationTest{

    private static final UUID MANAGER_ID = UUID.fromString("00000000-0000-4000-a000-000000000002");

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
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    JpaManagerRepository managerRepository;

    @BeforeEach
    void seedManager() {
        managerRepository.deleteAll();
        managerRepository.save(new ManagerEntity(MANAGER_ID, "Manager Autosalon", "manager"));
    }

    @Test
    void anonymousRequestShouldBe401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanCreateOrderAndOnlySeeOwn() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        String createdJson = mockMvc.perform(createRequest(userA, "IN_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", equalTo(OrderType.IN_STOCK.name())))
                .andExpect(jsonPath("$.status", equalTo("CREATED")))
                .andExpect(jsonPath("$.clientId", equalTo(userA.toString())))
                .andExpect(jsonPath("$.managerId", equalTo(MANAGER_ID.toString())))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        UUID orderId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

        mockMvc.perform(get("/api/orders").with(asUser(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));

        mockMvc.perform(get("/api/orders").with(asUser(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(0)));

        mockMvc.perform(get("/api/orders/" + orderId).with(asUser(userB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanSeeAllOrders() throws Exception {
        mockMvc.perform(createRequest(UUID.randomUUID(), "CUSTOM"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders").with(asManager(MANAGER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", equalTo(1)));
    }

    @Test
    void inStockOrderGoesThroughFullStatusChain() throws Exception {
        UUID client = UUID.randomUUID();

        String createdJson = mockMvc.perform(createRequest(client, "IN_STOCK"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

        mockMvc.perform(post("/api/orders/" + orderId + "/approve").with(asManager(MANAGER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("APPROVED_BY_MANAGER")));

        mockMvc.perform(post("/api/orders/" + orderId + "/invoice").with(asManager(MANAGER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("AWAIT_FOR_PAYMENT")));

        mockMvc.perform(post("/api/orders/" + orderId + "/pay").with(asUser(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("PAID")));
    }

    @Test
    void paymentBeforeApprovalIsRejected() throws Exception {
        UUID client = UUID.randomUUID();

        String createdJson = mockMvc.perform(createRequest(client, "IN_STOCK"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

        mockMvc.perform(post("/api/orders/" + orderId + "/pay").with(asUser(client)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customOrderIsNotApprovedByManager() throws Exception {
        String createdJson = mockMvc.perform(createRequest(UUID.randomUUID(), "CUSTOM"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

        mockMvc.perform(post("/api/orders/" + orderId + "/approve").with(asManager(MANAGER_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void foreignManagerCannotApproveOrder() throws Exception {
        String createdJson = mockMvc.perform(createRequest(UUID.randomUUID(), "IN_STOCK"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(objectMapper.readTree(createdJson).get("id").asText());

        mockMvc.perform(post("/api/orders/" + orderId + "/approve").with(asManager(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder createRequest(UUID client, String type) throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setType(type);
        req.setCarModelId(UUID.randomUUID());

        return post("/api/orders")
                .with(asUser(client))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asUser(UUID id) {
        return jwt().jwt(b -> b.subject(id.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor asManager(UUID id) {
        return jwt().jwt(b -> b.subject(id.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_MANAGER"));
    }
}
