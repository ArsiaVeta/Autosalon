package ru.arslanova.orderservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arslanova.orderservice.application.MessageIdempotencyService;
import ru.arslanova.orderservice.application.OrderService;
import ru.arslanova.orderservice.event.OrderApprovedEvent;
import ru.arslanova.orderservice.event.OrderRejectedEvent;
import ru.arslanova.orderservice.listener.StorageEventListener;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StorageEventListenerIdempotencyTest {

    @Mock
    OrderService orderService;

    @Mock
    MessageIdempotencyService idempotencyService;

    @InjectMocks
    StorageEventListener listener;

    @Test
    void duplicateApprovedEventStatusOnlyOnce() {
        OrderApprovedEvent event = approvedEvent();
        when(idempotencyService.alreadyProcessed(event.getEventId())).thenReturn(false, true);

        listener.handleApproved(event);
        listener.handleApproved(event);

        verify(orderService, times(1)).markReadyForPickup(event.getOrderId());
        verify(idempotencyService, times(1)).markProcessed(event.getEventId());
    }

    @Test
    void duplicateRejectedEventCancelsOrderOnlyOnce() {
        OrderRejectedEvent event = rejectedEvent();
        when(idempotencyService.alreadyProcessed(event.getEventId())).thenReturn(false, true);

        listener.handleRejected(event);
        listener.handleRejected(event);

        verify(orderService, times(1)).cancelByStorage(event.getOrderId());
        verify(idempotencyService, times(1)).markProcessed(event.getEventId());
    }

    @Test
    void alreadyProcessedApprovedEventDoesNotTouchOrder() {
        OrderApprovedEvent event = approvedEvent();
        when(idempotencyService.alreadyProcessed(event.getEventId())).thenReturn(true);

        listener.handleApproved(event);

        verify(orderService, never()).markReadyForPickup(any());
        verify(idempotencyService, never()).markProcessed(any());
    }

    private OrderApprovedEvent approvedEvent() {
        OrderApprovedEvent event = new OrderApprovedEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setTraceId("trace-" + UUID.randomUUID());
        event.setCreatedAt(Instant.now());
        return event;
    }

    private OrderRejectedEvent rejectedEvent() {
        OrderRejectedEvent event = new OrderRejectedEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setTraceId("trace-" + UUID.randomUUID());
        event.setCreatedAt(Instant.now());
        return event;
    }
}
