package ru.arslanova.orderservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.Order.OrderStatus;
import ru.arslanova.orderservice.domain.Order.OrderType;
import ru.arslanova.orderservice.domain.repository.OrderRepository;
import ru.arslanova.orderservice.event.OrderApprovedEvent;
import ru.arslanova.orderservice.event.OrderRejectedEvent;
import ru.arslanova.orderservice.infrastructure.repository.ProcessedMessageRepository;
import ru.arslanova.orderservice.listener.StorageEventListener;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "app.kafka.topics.auto-create=false"
})
public class OrderStatusIdempotencyIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    StorageEventListener listener;

    @Autowired
    ProcessedMessageRepository processedMessageRepository;

    @BeforeEach
    void cleanProcessedMessages() {
        processedMessageRepository.deleteAll();
    }

    @Test
    void duplicateApprovedEventMustNotChangeStatusAgain() {
        OrderContext order = orderRepository.save(newOrder(OrderStatus.PAID));
        OrderApprovedEvent event = approvedEvent(order.getId());

        listener.handleApproved(event);

        assertThat(statusOf(order.getId())).isEqualTo(OrderStatus.READY_FOR_PICKUP);
        assertThat(processedMessageRepository.existsByEventId(event.getEventId())).isTrue();

        OrderContext current = orderRepository.findById(order.getId());
        current.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(current);

        listener.handleApproved(event);

        assertThat(statusOf(order.getId())).isEqualTo(OrderStatus.CANCELLED);
        assertThat(processedMessageRepository.count()).isEqualTo(1);
    }


    private OrderStatus statusOf(UUID orderId) {
        return orderRepository.findById(orderId).getStatus();
    }

    private OrderContext newOrder(OrderStatus status) {
        return new OrderContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                OrderType.IN_STOCK,
                status,
                null
        );
    }

    private OrderApprovedEvent approvedEvent(UUID orderId) {
        OrderApprovedEvent event = new OrderApprovedEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(orderId);
        event.setTraceId("trace-" + UUID.randomUUID());
        event.setCreatedAt(Instant.now());
        return event;
    }

    private OrderRejectedEvent rejectedEvent(UUID orderId) {
        OrderRejectedEvent event = new OrderRejectedEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(orderId);
        event.setTraceId("trace-" + UUID.randomUUID());
        event.setCreatedAt(Instant.now());
        return event;
    }
}
