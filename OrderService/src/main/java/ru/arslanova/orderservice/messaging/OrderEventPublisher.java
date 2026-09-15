package ru.arslanova.orderservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.arslanova.orderservice.api.config.KafkaConfig;
import ru.arslanova.orderservice.event.OrderSentForApprovalEvent;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    public static final String TRACE_HEADER = KafkaConfig.TRACE_HEADER;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(OrderSentForApprovalEvent event) {
        String traceId = event.getTraceId();

        ProducerRecord<String, Object> record = new ProducerRecord<>(
                KafkaConfig.ORDER_SENT_TOPIC,
                String.valueOf(event.getOrderId()),
                event
        );

        if (traceId != null) {
            record.headers().add(TRACE_HEADER, traceId.getBytes(StandardCharsets.UTF_8));
        }

        kafkaTemplate.send(record).join();

        log.info("Published OrderSentForApproval, orderId={} traceId={}", event.getOrderId(), traceId);
    }
}
