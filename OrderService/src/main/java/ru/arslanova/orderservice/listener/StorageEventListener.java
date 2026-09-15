package ru.arslanova.orderservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.arslanova.orderservice.application.MessageIdempotencyService;
import ru.arslanova.orderservice.application.OrderService;
import ru.arslanova.orderservice.api.config.KafkaConfig;
import ru.arslanova.orderservice.event.OrderAcceptedEvent;
import ru.arslanova.orderservice.event.OrderApprovedEvent;
import ru.arslanova.orderservice.event.OrderPhase;
import ru.arslanova.orderservice.event.OrderRejectedEvent;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageEventListener {

    private final OrderService orderService;
    private final MessageIdempotencyService idempotencyService;

    @KafkaListener(topics = KafkaConfig.ORDER_APPROVED_TOPIC)
    public void handleApproved(OrderApprovedEvent event){
        String traceId = event.getTraceId() != null ? event.getTraceId() : UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        MDC.put("orderId", String.valueOf(event.getOrderId()));
        try {
            if (idempotencyService.alreadyProcessed(event.getEventId())) {
                log.info("DUPLICATE EVENT {}", event.getOrderId());
                return;
            }

            if (event.getPhase() == OrderPhase.APPROVAL) {
                log.info("ORDER APPROVED BY STORAGE {}", event.getOrderId());
                orderService.approveByStorage(event.getOrderId());
            } else {
                log.info("ORDER READY FOR PICKUP {}", event.getOrderId());
                orderService.markReadyForPickup(event.getOrderId());
            }

            idempotencyService.markProcessed(event.getEventId());
        } finally{
            MDC.remove("traceId");
            MDC.remove("orderId");
        }
    }

    @KafkaListener(topics = KafkaConfig.ORDER_ACCEPTED_TOPIC)
    public void handleAccepted(OrderAcceptedEvent event){
        String traceId = event.getTraceId() != null ? event.getTraceId() : UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        MDC.put("orderId", String.valueOf(event.getOrderId()));

        try {
            if (idempotencyService.alreadyProcessed(event.getEventId())) {
                log.info("DUPLICATE EVENT {}", event.getOrderId());
                return;
            }

            log.info("ORDER ACCEPTED FOR ASSEMBLY {} assemblyOrderId={}",
                    event.getOrderId(), event.getAssemblyOrderId());

            orderService.markAwaitingDelivery(event.getOrderId());

            idempotencyService.markProcessed(event.getEventId());
        } finally {
            MDC.remove("traceId");
            MDC.remove("orderId");
        }
    }

    @KafkaListener(topics = KafkaConfig.ORDER_REJECTED_TOPIC)
    public void handleRejected(OrderRejectedEvent event){
        String traceId = event.getTraceId() != null ? event.getTraceId() : UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        MDC.put("orderId", String.valueOf(event.getOrderId()));

        try {
            if (idempotencyService.alreadyProcessed(event.getEventId())) {
                log.info("DUPLICATE EVENT {}", event.getOrderId());
                return;
            }

            log.info("ORDER REJECTED {}", event.getOrderId());

            orderService.cancelByStorage(event.getOrderId());

            idempotencyService.markProcessed(event.getEventId());
        } finally {
            MDC.remove("traceId");
            MDC.remove("orderId");
        }
    }
}
