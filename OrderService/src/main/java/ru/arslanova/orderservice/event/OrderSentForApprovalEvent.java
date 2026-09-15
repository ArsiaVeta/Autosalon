package ru.arslanova.orderservice.event;

import lombok.Getter;
import lombok.Setter;
import ru.arslanova.orderservice.domain.Order.OrderType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
public class OrderSentForApprovalEvent {
    private UUID eventId;
    private UUID orderId;
    private UUID carModelId;
    private OrderType orderType;
    private Map<String, UUID> components;
    private String traceId;
    private Instant createdAt;
    private OrderPhase phase;


}
