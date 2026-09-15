package ru.arslanova.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import ru.arslanova.orderservice.api.config.JpaConfig;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.Order.OrderStatus;
import ru.arslanova.orderservice.domain.Order.OrderType;
import ru.arslanova.orderservice.infrastructure.mappers.OrderPersistenceMapper;
import ru.arslanova.orderservice.infrastructure.repository.JpaCustomOrderRepositoryAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaCustomOrderRepositoryAdapter.class, OrderPersistenceMapper.class, JpaConfig.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false"
})
public class OrderRepositoryIntegrationTest extends AbstractPostgresIntegrationTest{

    @Autowired
    JpaCustomOrderRepositoryAdapter repository;

    @Test
    void shouldPersistAndFindOrder() {
        Map<String, UUID> comps = new HashMap<>();
        comps.put("WHEEL", UUID.randomUUID());

        OrderContext order = new OrderContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                OrderType.CUSTOM,
                OrderStatus.AWAIT_FOR_PAYMENT,
                comps
        );

        OrderContext saved = repository.save(order);
        assertThat(saved.getId()).isNotNull();

        OrderContext loaded = repository.findById(saved.getId());
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.AWAIT_FOR_PAYMENT);
        assertThat(loaded.getType()).isEqualTo(OrderType.CUSTOM);
        assertThat(loaded.getComponents()).containsKey("WHEEL");
    }

    @Test
    void shouldFindAllByClientId() {
        UUID clientId = UUID.randomUUID();

        for (int i = 0; i < 3; i++) {
            OrderContext order = new OrderContext(
                    UUID.randomUUID(),
                    clientId,
                    null,
                    UUID.randomUUID(),
                    OrderType.IN_STOCK,
                    OrderStatus.AWAIT_FOR_PAYMENT,
                    null
            );
            repository.save(order);
        }

        var page = repository.findAllByClientId(clientId, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }
}
