package ru.arslanova.storageservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.arslanova.storageservice.application.service.AssemblyOrderService;
import ru.arslanova.storageservice.application.service.CarAvailabilityService;
import ru.arslanova.storageservice.application.service.MessageIdempotencyService;
import ru.arslanova.storageservice.config.KafkaConfig;
import ru.arslanova.storageservice.event.OrderAcceptedEvent;
import ru.arslanova.storageservice.event.OrderApprovedEvent;
import ru.arslanova.storageservice.event.OrderPhase;
import ru.arslanova.storageservice.event.OrderRejectedEvent;
import ru.arslanova.storageservice.event.OrderSentForApprovalEvent;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.AssemblyState;
import ru.arslanova.storageservice.infrastructure.entity.assambleStates.CarAssemblyOrder;
import ru.arslanova.storageservice.messaging.OrderEventPublisher;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private static final String CUSTOM_ORDER_TYPE = "CUSTOM";

    private final AssemblyOrderService assemblyOrderService;
    private final OrderEventPublisher publisher;
    private final MessageIdempotencyService idempotencyService;
    private final CarAvailabilityService carAvailabilityService;

    @KafkaListener(topics = KafkaConfig.ORDER_SENT_TOPIC)
    public void handleOrderSent(OrderSentForApprovalEvent event) {
        String traceId = event.getTraceId() != null ? event.getTraceId() : UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        try {
            MDC.put("orderId", String.valueOf(event.getOrderId()));
            if (idempotencyService.alreadyProcessed(event.getEventId())) {
                log.info("Duplicate event received, skipping. eventId={} orderId={} traceId={}",
                        event.getEventId(), event.getOrderId(), traceId);
                return;
            }

            OrderPhase phase = event.getPhase() != null ? event.getPhase() : OrderPhase.ASSEMBLY;

            log.info("Received OrderSentForApproval. orderId={} phase={} traceId={}",
                    event.getOrderId(), phase, traceId);

            if (phase == OrderPhase.APPROVAL) {
                handleApprovalRequest(event, traceId);
            } else {
                handleAssemblyRequest(event, traceId);
            }

            idempotencyService.markProcessed(event.getEventId());
        } finally {
            MDC.remove("traceId");
            MDC.remove("orderId");
        }
    }

    private void handleApprovalRequest(OrderSentForApprovalEvent event, String traceId) {
        try {
            carAvailabilityService.ensureAvailable(event.getCarModelId(), event.getOrderType());

            publisher.publishApproved(approvedEvent(event.getOrderId(), traceId, OrderPhase.APPROVAL));

            log.info("Configuration approved by storage. orderId={} traceId={}",
                    event.getOrderId(), traceId);
        } catch (Exception e) {
            log.error("Configuration rejected by storage. orderId={} traceId={}",
                    event.getOrderId(), traceId, e);

            publisher.publishRejected(rejectedEvent(event.getOrderId(), traceId, e.getMessage()));
        }
    }

    private void handleAssemblyRequest(OrderSentForApprovalEvent event, String traceId) {
        CarAssemblyOrder assembly = null;
        try {
            carAvailabilityService.ensureAvailable(event.getCarModelId(), event.getOrderType());

            assembly = assemblyOrderService.create(event.getOrderId());

            if (CUSTOM_ORDER_TYPE.equalsIgnoreCase(event.getOrderType())) {
                OrderAcceptedEvent accepted = new OrderAcceptedEvent();
                accepted.setEventId(UUID.randomUUID());
                accepted.setOrderId(event.getOrderId());
                accepted.setAssemblyOrderId(assembly.getId());
                accepted.setTraceId(traceId);
                accepted.setCreatedAt(Instant.now());
                publisher.publishAccepted(accepted);
            }

            assemblyOrderService.updateState(assembly.getId(), AssemblyState.ASSEMBLED);

            publisher.publishApproved(approvedEvent(event.getOrderId(), traceId, OrderPhase.ASSEMBLY));

            log.info("Assembly finished. orderId={} traceId={}", event.getOrderId(), traceId);
        } catch (Exception e) {
            log.error("Assembly failed, publishing OrderRejected. orderId={} traceId={}",
                    event.getOrderId(), traceId, e);

            if (assembly != null) {
                try {
                    assemblyOrderService.updateState(assembly.getId(), AssemblyState.FAIL);
                } catch (Exception inner) {
                    log.warn("Could not mark assembly as FAIL, assemblyId={} traceId={}",
                            assembly.getId(), traceId, inner);
                }
            }

            publisher.publishRejected(rejectedEvent(event.getOrderId(), traceId, e.getMessage()));
        }
    }

    private OrderApprovedEvent approvedEvent(UUID orderId, String traceId, OrderPhase phase) {
        OrderApprovedEvent approved = new OrderApprovedEvent();
        approved.setEventId(UUID.randomUUID());
        approved.setOrderId(orderId);
        approved.setTraceId(traceId);
        approved.setCreatedAt(Instant.now());
        approved.setPhase(phase);
        return approved;
    }

    private OrderRejectedEvent rejectedEvent(UUID orderId, String traceId, String reason) {
        OrderRejectedEvent rejected = new OrderRejectedEvent();
        rejected.setEventId(UUID.randomUUID());
        rejected.setOrderId(orderId);
        rejected.setTraceId(traceId);
        rejected.setCreatedAt(Instant.now());
        rejected.setReason(reason);
        return rejected;
    }
}
