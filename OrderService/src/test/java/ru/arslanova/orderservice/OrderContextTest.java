package ru.arslanova.orderservice;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.Order.OrderStatus;
import ru.arslanova.orderservice.domain.Order.OrderType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderContextTest {

    @Test
    void shouldGenerateIdEithConstructedWithoutId(){
        OrderContext order = new OrderContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                OrderType.IN_STOCK, OrderStatus.AWAIT_FOR_PAYMENT, null
        );

        Assertions.assertThat(order.getId()).isNotNull();
        Assertions.assertThat(order.getComponents()).isEmpty();
    }

    @Test
    void shouldPreserveProvidedComponents() {
        Map<String, UUID> comps = new HashMap<>();
        comps.put("WHEEL", UUID.randomUUID());

        OrderContext order = new OrderContext(
                UUID.randomUUID(), null, UUID.randomUUID(),
                OrderType.CUSTOM, OrderStatus.AWAIT_FOR_PAYMENT, comps
        );

        assertThat(order.getType()).isEqualTo(OrderType.CUSTOM);
        assertThat(order.getComponents()).containsKey("WHEEL");
    }

    @Test
    void shouldAllowStatusTransaction() {
        OrderContext order = new OrderContext(
                UUID.randomUUID(), null, UUID.randomUUID(),
                OrderType.IN_STOCK, OrderStatus.AWAIT_FOR_PAYMENT, null
        );
        order.transitionTo(OrderStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

        order.transitionTo(OrderStatus.READY_FOR_PICKUP);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
    }
}
