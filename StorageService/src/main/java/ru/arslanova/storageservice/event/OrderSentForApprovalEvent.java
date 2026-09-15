package ru.arslanova.storageservice.event;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Setter
@Getter
public class OrderSentForApprovalEvent {
    private UUID eventId;
    private UUID orderId;
    private UUID carModelId;
    private String orderType;
    private Map<String, UUID> components;
    private String traceId;
    private Instant createdAt;
    private OrderPhase phase;


}
