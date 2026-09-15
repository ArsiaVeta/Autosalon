package ru.arslanova.orderservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import ru.arslanova.orderservice.application.OrderService;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.Order.OrderStatus;
import ru.arslanova.orderservice.domain.Order.OrderType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.arslanova.orderservice.domain.repository.OrderRepository;
import ru.arslanova.orderservice.infrastructure.entity.OutboxEventEntity;
import ru.arslanova.orderservice.infrastructure.repository.OutboxEventRepository;
import ru.arslanova.orderservice.messaging.OrderEventPublisher;
import ru.arslanova.orderservice.outbox.OutboxScheduler;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.topics.auto-create=false"
})
public class OutboxTransactionRollbackIntegrationTest extends AbstractPostgresIntegrationTest{

    @MockBean
    OrderEventPublisher publisher;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxEventRepository outboxRepository;

    @Autowired
    OrderService orderService;

    @Autowired
    PlatformTransactionManager transactionManager;


    @Autowired
    OutboxScheduler outboxScheduler;

    private TransactionTemplate transactionTemplate;


    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eventIsNotPublishedWhenStatusTransactionRollBack() {
        OrderContext order = orderRepository.save(newOrder());
        UUID orderId = order.getId();

        transactionTemplate.execute(status -> {
            orderService.pay(orderId);
            status.setRollbackOnly();
            return null;
        });

        assertThat(orderRepository.findById(orderId).getStatus())
                .isEqualTo(OrderStatus.AWAIT_FOR_PAYMENT);

        assertThat(outboxFor(orderId)).isEmpty();

        outboxScheduler.publishEvents();
        verify(publisher, never()).publish(argThat(e -> orderId.equals(e.getOrderId())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void eventIsNotPublishedWhenStatusTransactionCommits() {
        OrderContext order = orderRepository.save(newOrder());
        UUID orderId = order.getId();

        transactionTemplate.execute(status -> {
            orderService.pay(orderId);
            return null;
        });

        assertThat(orderRepository.findById(orderId).getStatus())
                .isEqualTo(OrderStatus.PAID);

        List<OutboxEventEntity> pending = outboxFor(orderId);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEventType()).isEqualTo("ORDER_SENT");
        assertThat(pending.get(0).getTraceId()).isNotBlank();

        outboxScheduler.publishEvents();

        verify(publisher, atLeastOnce()).publish(argThat(e -> orderId.equals(e.getOrderId())));
        assertThat(outboxFor(orderId))
                .as("после успешной публикации запись помечается обработанной")
                .allMatch(OutboxEventEntity::isProcessed);
    }

    private List<OutboxEventEntity> outboxFor(UUID orderId) {
        return outboxRepository.findAll().stream()
                .filter(e -> orderId.equals(e.getAggregateId()))
                .toList();
    }
    private OrderContext newOrder() {
        return new OrderContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                OrderType.IN_STOCK,
                OrderStatus.AWAIT_FOR_PAYMENT,
                null
        );
    }
}
