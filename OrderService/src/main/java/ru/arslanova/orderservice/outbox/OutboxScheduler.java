package ru.arslanova.orderservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.arslanova.orderservice.event.OrderSentForApprovalEvent;
import ru.arslanova.orderservice.infrastructure.entity.OutboxEventEntity;
import ru.arslanova.orderservice.infrastructure.repository.OutboxEventRepository;
import ru.arslanova.orderservice.messaging.OrderEventPublisher;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final OutboxEventRepository repository;
    private final OrderEventPublisher publisher;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void publishEvents(){
        List<OutboxEventEntity> events =
                repository.findAllByProcessedFalseAndAttemptsLessThan(maxAttempts);

        for(OutboxEventEntity event : events){
            String traceId = event.getTraceId() != null
                    ? event.getTraceId()
                    : UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
            MDC.put("outboxEventId", String.valueOf(event.getId()));
            try {
                OrderSentForApprovalEvent payload =
                        objectMapper.readValue(
                                event.getPayload(),
                                OrderSentForApprovalEvent.class
                        );
                if (payload.getTraceId() == null || payload.getTraceId().isBlank()){
                    payload.setTraceId(traceId);
                }

                publisher.publish(payload);

                event.setProcessed(true);
                event.setLastError(null);
                repository.save(event);

                log.info("OUTBOX EVENT SENT id={} orderId={} attempts={} traceId={}",
                        event.getId(), event.getAggregateId(),
                        event.getAttempts(), traceId);
            } catch (Exception e){
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(truncate(e.getMessage()));
                repository.save(event);

                if (event.getAttempts() >= maxAttempts){
                    log.error("OUTBOX EVENT FAILED PERMANENTLY id={} orderId={} attempts={} traceId={}",
                            event.getId(), event.getAggregateId(),
                            event.getAttempts(), traceId, e);
                } else {
                    log.warn("OUTBOX EVENT PUBLISH FAILED id={} orderId={} attempts={}/{} traceId={}",
                            event.getId(), event.getAggregateId(),
                            event.getAttempts(), maxAttempts, traceId, e);
                }
            } finally {
                MDC.remove("traceId");
                MDC.remove("outboxEventId");
            }
        }
    }

    private String truncate(String s){
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
