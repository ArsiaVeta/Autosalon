package ru.arslanova.orderservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arslanova.orderservice.application.ManagerAssignmentService;
import ru.arslanova.orderservice.application.OrderService;
import ru.arslanova.orderservice.domain.Order.OrderContext;
import ru.arslanova.orderservice.domain.Order.OrderStatus;
import ru.arslanova.orderservice.domain.Order.OrderType;
import ru.arslanova.orderservice.domain.exeptions.DomainValidationExeption;
import ru.arslanova.orderservice.domain.repository.OrderRepository;
import ru.arslanova.orderservice.event.OrderPhase;
import ru.arslanova.orderservice.event.OrderSentForApprovalEvent;
import ru.arslanova.orderservice.outbox.OutboxService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServicePayTest {
    @Mock OrderRepository repository;
    @Mock
    OutboxService outboxService;
    @Mock
    ManagerAssignmentService managerAssignmentService;

    @InjectMocks
    OrderService service;

    @Test
    void createAssignsManagerAndStartsInCreatedStatus() {
        UUID clientId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        OrderContext order = new OrderContext(
                clientId, null, UUID.randomUUID(),
                OrderType.IN_STOCK, null, null
        );
        when(managerAssignmentService.assign(any())).thenReturn(managerId);
        when(repository.save(any(OrderContext.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderContext created = service.create(order);

        assertThat(created.getManagerId()).isEqualTo(managerId);
        assertThat(created.getClientId()).isEqualTo(clientId);
        assertThat(created.getStatus()).isEqualTo(OrderStatus.CREATED);
        verify(outboxService, never()).saveEvent(any());
    }

    @Test
    void createdCustomOrderIsSentToStorageForApproval() {
        OrderContext order = new OrderContext(
                UUID.randomUUID(), null, UUID.randomUUID(),
                OrderType.CUSTOM, null, null
        );
        when(managerAssignmentService.assign(any())).thenReturn(UUID.randomUUID());
        when(repository.save(any(OrderContext.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderContext created = service.create(order);

        ArgumentCaptor<OrderSentForApprovalEvent> event =
                ArgumentCaptor.forClass(OrderSentForApprovalEvent.class);
        verify(outboxService).saveEvent(event.capture());
        assertThat(event.getValue().getPhase()).isEqualTo(OrderPhase.APPROVAL);
        assertThat(event.getValue().getOrderId()).isEqualTo(created.getId());
    }

    @Test
    void payShouldSetPaidStatusAndEmitOutboxEvent(){
        UUID orderId = UUID.randomUUID();
        OrderContext order = new OrderContext(
                orderId, UUID.randomUUID(), null, UUID.randomUUID(),
                OrderType.IN_STOCK, OrderStatus.AWAIT_FOR_PAYMENT, null
        );
        when(repository.findById(orderId)).thenReturn(order);
        when(repository.save(any(OrderContext.class))).thenAnswer(inv -> inv.getArgument(0));

        service.pay(orderId);

        ArgumentCaptor<OrderContext> savedOrder = ArgumentCaptor.forClass(OrderContext.class);
        verify(repository).save(savedOrder.capture());
        assertThat(savedOrder.getValue().getStatus()).isEqualTo(OrderStatus.PAID);

        ArgumentCaptor<OrderSentForApprovalEvent> event =
                ArgumentCaptor.forClass(OrderSentForApprovalEvent.class);
        verify(outboxService).saveEvent(event.capture());
        assertThat(event.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(event.getValue().getEventId()).isNotNull();
        assertThat(event.getValue().getTraceId()).isNotBlank();
        assertThat(event.getValue().getCreatedAt()).isNotNull();
        assertThat(event.getValue().getPhase()).isEqualTo(OrderPhase.ASSEMBLY);
    }

    @Test
    void payIsRejectedBeforeInvoice(){
        UUID orderId = UUID.randomUUID();
        OrderContext order = new OrderContext(
                orderId, UUID.randomUUID(), null, UUID.randomUUID(),
                OrderType.IN_STOCK, OrderStatus.CREATED, null
        );
        when(repository.findById(orderId)).thenReturn(order);

        assertThatThrownBy(() -> service.pay(orderId))
                .isInstanceOf(DomainValidationExeption.class);

        verify(repository, never()).save(any());
        verify(outboxService, never()).saveEvent(any());
    }

    @Test
    void markReadyForPickupShouldUpdateStatus() {
        UUID orderId = UUID.randomUUID();
        OrderContext order = new OrderContext(
                orderId, UUID.randomUUID(), null, UUID.randomUUID(),
                OrderType.CUSTOM, OrderStatus.AWAIT_DELIVERY, null
        );
        when(repository.findById(orderId)).thenReturn(order);
        when(repository.save(any(OrderContext.class))).thenAnswer(inv -> inv.getArgument(0));

        service.markReadyForPickup(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
        verify(repository).save(order);
    }

    @Test
    void markAwaitingDeliveryIsSkippedForInStockOrder() {
        UUID orderId = UUID.randomUUID();
        OrderContext order = new OrderContext(
                orderId, UUID.randomUUID(), null, UUID.randomUUID(),
                OrderType.IN_STOCK, OrderStatus.PAID, null
        );
        when(repository.findById(orderId)).thenReturn(order);

        service.markAwaitingDelivery(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(repository, never()).save(any());
    }

    @Test
    void cancelShouldSetCancelledStatus() {
        UUID orderId = UUID.randomUUID();
        OrderContext order = new OrderContext(
                orderId, UUID.randomUUID(), null, UUID.randomUUID(),
                OrderType.IN_STOCK, OrderStatus.PAID, null
        );
        when(repository.findById(orderId)).thenReturn(order);
        when(repository.save(any(OrderContext.class))).thenAnswer(inv -> inv.getArgument(0));

        service.cancel(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void completeIsAllowedOnlyAfterPickupIsReady() {
        UUID orderId = UUID.randomUUID();
        OrderContext order = new OrderContext(
                orderId, UUID.randomUUID(), null, UUID.randomUUID(),
                OrderType.IN_STOCK, OrderStatus.READY_FOR_PICKUP, null
        );
        when(repository.findById(orderId)).thenReturn(order);
        when(repository.save(any(OrderContext.class))).thenAnswer(inv -> inv.getArgument(0));

        service.complete(orderId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
}
