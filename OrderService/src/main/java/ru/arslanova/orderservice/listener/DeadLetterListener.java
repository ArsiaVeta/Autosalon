package ru.arslanova.orderservice.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.arslanova.orderservice.api.config.KafkaConfig;

@Slf4j
@Component
public class DeadLetterListener {

    @KafkaListener(topics = KafkaConfig.ORDER_APPROVED_DLT, containerFactory = "dltKafkaListenerContainerFactory")
    public void approvedDlt(ConsumerRecord<String, String> record) {
        log.error("MESSAGE IN APPROVED DLT topic={} key={} value={}",
                record.topic(), record.key(), record.value());
    }

    @KafkaListener(topics = KafkaConfig.ORDER_REJECTED_DLT, containerFactory = "dltKafkaListenerContainerFactory")
    public void rejectedDlt(ConsumerRecord<String, String> record) {
        log.error("MESSAGE IN REJECTED DLT topic={} key={} value={}",
                record.topic(), record.key(), record.value());
    }

    @KafkaListener(topics = KafkaConfig.ORDER_ACCEPTED_DLT, containerFactory = "dltKafkaListenerContainerFactory")
    public void acceptedDlt(ConsumerRecord<String, String> record) {
        log.error("MESSAGE IN ACCEPTED DLT topic={} key={} value={}",
                record.topic(), record.key(), record.value());
    }
}
