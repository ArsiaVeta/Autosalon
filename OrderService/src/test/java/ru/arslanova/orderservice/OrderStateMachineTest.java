package ru.arslanova.orderservice;

import org.junit.jupiter.api.Test;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.Order.OrderStateMachine;
import ru.arslanova.orderservice.domain.Order.OrderStatus;
import ru.arslanova.orderservice.domain.Order.OrderType;
import ru.arslanova.orderservice.domain.exeptions.DomainValidationExeption;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrderStateMachineTest {

    @Test
    void inStockOrderFollowsFullChain() {
        OrderContext order = newOrder(OrderType.IN_STOCK);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);

        order.transitionTo(OrderStatus.APPROVED_BY_MANAGER);
        order.transitionTo(OrderStatus.AWAIT_FOR_PAYMENT);
        order.transitionTo(OrderStatus.PAID);
        order.transitionTo(OrderStatus.READY_FOR_PICKUP);
        order.transitionTo(OrderStatus.COMPLETED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.isFinal()).isTrue();
    }

    @Test
    void customOrderFollowsFullChain() {
        OrderContext order = newOrder(OrderType.CUSTOM);

        order.transitionTo(OrderStatus.APPROVED_BY_STORAGE);
        order.transitionTo(OrderStatus.AWAIT_FOR_PAYMENT);
        order.transitionTo(OrderStatus.PAID);
        order.transitionTo(OrderStatus.AWAIT_DELIVERY);
        order.transitionTo(OrderStatus.READY_FOR_PICKUP);
        order.transitionTo(OrderStatus.COMPLETED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void inStockOrderIsNotApprovedByStorage() {
        OrderContext order = newOrder(OrderType.IN_STOCK);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.APPROVED_BY_STORAGE))
                .isInstanceOf(DomainValidationExeption.class);
    }

    @Test
    void customOrderIsNotApprovedByManager() {
        OrderContext order = newOrder(OrderType.CUSTOM);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.APPROVED_BY_MANAGER))
                .isInstanceOf(DomainValidationExeption.class);
    }

    @Test
    void inStockOrderHasNoDeliveryStep() {
        OrderContext order = newOrder(OrderType.IN_STOCK);
        order.transitionTo(OrderStatus.APPROVED_BY_MANAGER);
        order.transitionTo(OrderStatus.AWAIT_FOR_PAYMENT);
        order.transitionTo(OrderStatus.PAID);

        assertThat(order.canTransitionTo(OrderStatus.AWAIT_DELIVERY)).isFalse();
        assertThat(order.canTransitionTo(OrderStatus.READY_FOR_PICKUP)).isTrue();
    }

    @Test
    void customOrderGoesThroughDeliveryBeforePickup() {
        OrderContext order = newOrder(OrderType.CUSTOM);
        order.transitionTo(OrderStatus.APPROVED_BY_STORAGE);
        order.transitionTo(OrderStatus.AWAIT_FOR_PAYMENT);
        order.transitionTo(OrderStatus.PAID);

        assertThat(order.canTransitionTo(OrderStatus.READY_FOR_PICKUP)).isFalse();

        order.transitionTo(OrderStatus.AWAIT_DELIVERY);

        assertThat(order.canTransitionTo(OrderStatus.READY_FOR_PICKUP)).isTrue();
    }

    @Test
    void paymentIsNotAllowedBeforeApproval() {
        OrderContext order = newOrder(OrderType.IN_STOCK);

        assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAID))
                .isInstanceOf(DomainValidationExeption.class);
    }

    @Test
    void orderCanBeCancelledFromAnyActiveStatus() {
        for (OrderStatus status : OrderStatus.values()) {
            if (OrderStateMachine.isFinal(status)) {
                continue;
            }
            OrderContext order = newOrder(OrderType.CUSTOM);
            order.setStatus(status);

            order.transitionTo(OrderStatus.CANCELLED);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    @Test
    void finalStatusesAreTerminal() {
        OrderContext cancelled = newOrder(OrderType.IN_STOCK);
        cancelled.setStatus(OrderStatus.CANCELLED);

        OrderContext completed = newOrder(OrderType.IN_STOCK);
        completed.setStatus(OrderStatus.COMPLETED);

        assertThat(cancelled.canTransitionTo(OrderStatus.PAID)).isFalse();
        assertThat(cancelled.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        assertThat(completed.canTransitionTo(OrderStatus.READY_FOR_PICKUP)).isFalse();
    }

    private OrderContext newOrder(OrderType type) {
        return new OrderContext(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                OrderStateMachine.initialStatus(),
                null
        );
    }
}
