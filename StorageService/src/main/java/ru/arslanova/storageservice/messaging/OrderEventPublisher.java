package ru.arslanova.storageservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.arslanova.storageservice.config.KafkaConfig;
import ru.arslanova.storageservice.event.OrderAcceptedEvent;
import ru.arslanova.storageservice.event.OrderApprovedEvent;
import ru.arslanova.storageservice.event.OrderRejectedEvent;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    public static final String TRACE_HEADER = KafkaConfig.TRACE_HEADER;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishApproved(OrderApprovedEvent event) {
        send(KafkaConfig.ORDER_APPROVED_TOPIC, String.valueOf(event.getOrderId()), event, event.getTraceId());
        log.info("Published OrderApproved, orderId={} traceId={}", event.getOrderId(), event.getTraceId());
    }

    public void publishRejected(OrderRejectedEvent event) {
        send(KafkaConfig.ORDER_REJECTED_TOPIC, String.valueOf(event.getOrderId()), event, event.getTraceId());
        log.info("Published OrderRejected, orderId={} traceId={}", event.getOrderId(), event.getTraceId());
    }

    public void publishAccepted(OrderAcceptedEvent event) {
        send(KafkaConfig.ORDER_ACCEPTED_TOPIC, String.valueOf(event.getOrderId()), event, event.getTraceId());
        log.info("Published OrderAccepted, orderId={} traceId={}", event.getOrderId(), event.getTraceId());
    }

    private void send(String topic, String key, Object payload, String traceId) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, payload);

        if (traceId != null) {
            record.headers().add(TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
        }

        kafkaTemplate.send(record).join();
    }
}
