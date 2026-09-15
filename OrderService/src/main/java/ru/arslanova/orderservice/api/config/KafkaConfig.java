package ru.arslanova.orderservice.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String ORDER_SENT_TOPIC = "order.sent";
    public static final String ORDER_APPROVED_TOPIC = "order.approved";
    public static final String ORDER_REJECTED_TOPIC = "order.rejected";
    public static final String ORDER_ACCEPTED_TOPIC = "order.accepted";

    public static final String ORDER_APPROVED_DLT = ORDER_APPROVED_TOPIC + ".DLT";
    public static final String ORDER_REJECTED_DLT = ORDER_REJECTED_TOPIC + ".DLT";
    public static final String ORDER_ACCEPTED_DLT = ORDER_ACCEPTED_TOPIC + ".DLT";

    public static final String DLT_SUFFIX = ".DLT";
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:order-service}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.listener.auto-startup:true}")
    private boolean listenerAutoStartup;

    @Value("${spring.kafka.listener.concurrency:1}")
    private int listenerConcurrency;

    @Value("${app.kafka.producer.max-block-ms:5000}")
    private int producerMaxBlockMs;

    @Value("${app.kafka.retry.attempts:2}")
    private long retryAttempts;

    @Value("${app.kafka.retry.interval-ms:3000}")
    private long retryIntervalMs;

    @Configuration
    @ConditionalOnProperty(name = "app.kafka.topics.auto-create", havingValue = "true", matchIfMissing = true)
    public static class Topics {

        @Bean
        public NewTopic orderSentTopic() {
            return TopicBuilder.name(ORDER_SENT_TOPIC).partitions(1).replicas(1).build();
        }

        @Bean
        public NewTopic orderApprovedTopic() {
            return TopicBuilder.name(ORDER_APPROVED_TOPIC).partitions(1).replicas(1).build();
        }

        @Bean
        public NewTopic orderRejectedTopic() {
            return TopicBuilder.name(ORDER_REJECTED_TOPIC).partitions(1).replicas(1).build();
        }

        @Bean
        public NewTopic orderAcceptedTopic() {
            return TopicBuilder.name(ORDER_ACCEPTED_TOPIC).partitions(1).replicas(1).build();
        }

        @Bean
        public NewTopic orderAcceptedDlt() {
            return TopicBuilder.name(ORDER_ACCEPTED_DLT).partitions(1).replicas(1).build();
        }

        @Bean
        public NewTopic orderApprovedDlt() {
            return TopicBuilder.name(ORDER_APPROVED_DLT).partitions(1).replicas(1).build();
        }

        @Bean
        public NewTopic orderRejectedDlt() {
            return TopicBuilder.name(ORDER_REJECTED_DLT).partitions(1).replicas(1).build();
        }
    }

    @Bean
    public ProducerFactory<String, Object> eventProducerFactory(ObjectMapper objectMapper) {
        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(producerConfig(), new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> eventProducerFactory) {
        return new KafkaTemplate<>(eventProducerFactory);
    }

    @Bean
    public ProducerFactory<String, String> dltProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfig(), new StringSerializer(), new StringSerializer());
    }

    @Bean
    public KafkaTemplate<String, String> dltKafkaTemplate(ProducerFactory<String, String> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), new StringDeserializer());
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dltKafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1)
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(retryIntervalMs, retryAttempts));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler,
            ObjectMapper objectMapper) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setRecordMessageConverter(new StringJsonMessageConverter(objectMapper));
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setAutoStartup(listenerAutoStartup);
        factory.setConcurrency(listenerConcurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setAutoStartup(listenerAutoStartup);
        factory.setConcurrency(listenerConcurrency);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    private Map<String, Object> producerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, producerMaxBlockMs);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, producerMaxBlockMs * 2);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, producerMaxBlockMs);
        return config;
    }
}
