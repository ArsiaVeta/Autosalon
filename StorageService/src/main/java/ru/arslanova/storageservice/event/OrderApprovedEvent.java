package ru.arslanova.storageservice.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
public class OrderApprovedEvent {
    private UUID eventId;
    private UUID orderId;
    private String traceId;
    private Instant createdAt;
    private OrderPhase phase;
}
