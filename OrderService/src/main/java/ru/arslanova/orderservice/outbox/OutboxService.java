package ru.arslanova.orderservice.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.arslanova.orderservice.event.OrderSentForApprovalEvent;
import ru.arslanova.orderservice.infrastructure.entity.OutboxEventEntity;
import ru.arslanova.orderservice.infrastructure.repository.OutboxEventRepository;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public void saveEvent(OrderSentForApprovalEvent event){
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventEntity entity =
                    new OutboxEventEntity(
                            event.getOrderId(),
                            "ORDER_SENT",
                            payload,
                            event.getTraceId()
                    );
            repository.save(entity);

        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
