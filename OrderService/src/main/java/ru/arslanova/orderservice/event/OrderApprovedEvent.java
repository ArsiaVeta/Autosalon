package ru.arslanova.orderservice.event;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
public class OrderApprovedEvent {
    private UUID eventId;
    private UUID orderId;
    private String traceId;
    private Instant createdAt;
    private OrderPhase phase;
}
