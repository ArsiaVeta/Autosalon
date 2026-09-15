package ru.arslanova.storageservice.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.arslanova.storageservice.config.KafkaConfig;

@Slf4j
@Component
public class DeadLetterListener {

    @KafkaListener(topics = KafkaConfig.ORDER_SENT_DLT, containerFactory = "dltKafkaListenerContainerFactory")
    public void orderSentDlt(ConsumerRecord<String, String> record) {
        log.error("MESSAGE IN ORDER SENT DLT topic={} key={} value={}",
                record.topic(), record.key(), record.value());
    }
}
