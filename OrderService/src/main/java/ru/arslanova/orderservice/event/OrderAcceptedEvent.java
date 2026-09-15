package ru.arslanova.orderservice.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
public class OrderAcceptedEvent {
    private UUID eventId;
    private UUID orderId;
    private UUID assemblyOrderId;
    private String traceId;
    private Instant createdAt;
}
