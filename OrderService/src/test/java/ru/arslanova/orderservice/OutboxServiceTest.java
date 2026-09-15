package ru.arslanova.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arslanova.orderservice.event.OrderSentForApprovalEvent;
import ru.arslanova.orderservice.infrastructure.entity.OutboxEventEntity;
import ru.arslanova.orderservice.infrastructure.repository.OutboxEventRepository;
import ru.arslanova.orderservice.outbox.OutboxService;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OutboxServiceTest {
    @Mock
    OutboxEventRepository repository;

    @Test
    void saveEventShouldPersistSerializedPayload() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        OutboxService service = new OutboxService(repository, mapper);

        OrderSentForApprovalEvent event = new OrderSentForApprovalEvent();
        event.setEventId(UUID.randomUUID());
        event.setOrderId(UUID.randomUUID());
        event.setTraceId("trace-1");
        event.setCreatedAt(Instant.now());

        service.saveEvent(event);

        ArgumentCaptor<OutboxEventEntity> entity = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(entity.capture());
        assertThat(entity.getValue().getAggregateId()).isEqualTo(event.getOrderId());
        assertThat(entity.getValue().getEventType()).isEqualTo("ORDER_SENT");
        assertThat(entity.getValue().isProcessed()).isFalse();
        assertThat(entity.getValue().getPayload()).contains(event.getOrderId().toString());
    }
}
