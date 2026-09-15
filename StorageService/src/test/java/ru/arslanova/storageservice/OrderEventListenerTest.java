package ru.arslanova.storageservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arslanova.storageservice.application.service.AssemblyOrderService;
import ru.arslanova.storageservice.application.service.CarAvailabilityService;
import ru.arslanova.storageservice.application.service.MessageIdempotencyService;
import ru.arslanova.storageservice.domain.exeptions.CarNotAvailableException;
import ru.arslanova.storageservice.event.OrderApprovedEvent;
import ru.arslanova.storageservice.event.OrderRejectedEvent;
import ru.arslanova.storageservice.event.OrderSentForApprovalEvent;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.AssemblyState;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.CarAssemblyOrder;
import ru.arslanova.storageservice.listener.OrderEventListener;
import ru.arslanova.storageservice.messaging.OrderEventPublisher;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class OrderEventListenerTest {
    @Mock
    AssemblyOrderService assemblyOrderService;
    @Mock
    OrderEventPublisher publisher;
    @Mock
    MessageIdempotencyService idempotencyService;
    @Mock
    CarAvailabilityService carAvailabilityService;

    @InjectMocks
    OrderEventListener listener;

    @Test
    void handleShouldPublishApprovedOnSuccess() {
        OrderSentForApprovalEvent event = newEvent();

        CarAssemblyOrder assembly = new CarAssemblyOrder(event.getOrderId(), AssemblyState.CREATED);
        when(idempotencyService.alreadyProcessed(event.getEventId())).thenReturn(false);
        when(assemblyOrderService.create(event.getOrderId())).thenReturn(assembly);

        listener.handleOrderSent(event);

        ArgumentCaptor<OrderApprovedEvent> approved = ArgumentCaptor.forClass(OrderApprovedEvent.class);
        verify(carAvailabilityService).ensureAvailable(event.getCarModelId(), event.getOrderType());
        verify(publisher).publishApproved(approved.capture());
        assertThat(approved.getValue().getOrderId()).isEqualTo(event.getOrderId());
        assertThat(approved.getValue().getTraceId()).isEqualTo(event.getTraceId());
        verify(idempotencyService).markProcessed(event.getEventId());
    }

    @Test
    void handleShouldSkipDuplicateEvent() {
        OrderSentForApprovalEvent event = newEvent();
        when(idempotencyService.alreadyProcessed(event.getEventId())).thenReturn(true);

        listener.handleOrderSent(event);

        verify(assemblyOrderService, never()).create(any());
        verify(publisher, never()).publishApproved(any());
        verify(publisher, never()).publishRejected(any());
        verify(idempotencyService, never()).markProcessed(any());
    }

    @Test
    void handleShouldPublishRejectedOnFailure() {
        OrderSentForApprovalEvent event = newEvent();
        when(idempotencyService.alreadyProcessed(event.getEventId())).thenReturn(false);
        when(assemblyOrderService.create(event.getOrderId()))
                .thenThrow(new RuntimeException("no parts"));

        listener.handleOrderSent(event);

        ArgumentCaptor<OrderRejectedEvent> rejected = ArgumentCaptor.forClass(OrderRejectedEvent.class);
        verify(publisher).publishRejected(rejected.capture());
        assertThat(rejected.getValue().getOrderId()).isEqualTo(event.getOrderId());
        assertThat(rejected.getValue().getReason()).isEqualTo("no parts");
        verify(publisher, never()).publishApproved(any());
        verify(idempotencyService).markProcessed(event.getEventId());
    }

    @Test
    void handleShouldPublishRejectedWhenCarNotAvailable() {
        OrderSentForApprovalEvent event = newEvent();
        when(idempotencyService.alreadyProcessed(event.getEventId())).thenReturn(false);
        doThrow(new CarNotAvailableException("Car not in stock"))
                .when(carAvailabilityService).ensureAvailable(event.getCarModelId(), event.getOrderType());

        listener.handleOrderSent(event);

        ArgumentCaptor<OrderRejectedEvent> rejected = ArgumentCaptor.forClass(OrderRejectedEvent.class);
        verify(publisher).publishRejected(rejected.capture());
        assertThat(rejected.getValue().getReason()).contains("Car not in stock");
        verify(assemblyOrderService, never()).create(any());
        verify(publisher, never()).publishApproved(any());
        verify(idempotencyService).markProcessed(event.getEventId());
    }

    private OrderSentForApprovalEvent newEvent() {
        OrderSentForApprovalEvent event = new OrderSentForApprovalEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setCarModelId(UUID.randomUUID());
        event.setOrderType("IN_STOCK");
        event.setTraceId("trace-" + UUID.randomUUID());
        event.setCreatedAt(Instant.now());
        return event;
    }

}
