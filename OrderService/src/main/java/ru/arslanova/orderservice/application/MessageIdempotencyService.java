package ru.arslanova.orderservice.application;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.arslanova.orderservice.infrastructure.entity.ProcessedMessageEntity;
import ru.arslanova.orderservice.infrastructure.repository.ProcessedMessageRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageIdempotencyService {
    private final ProcessedMessageRepository repository;

    public boolean alreadyProcessed(UUID eventId){
        return repository.existsByEventId(eventId);
    }

    @Transactional
    public void markProcessed(UUID eventId){
        repository.save(new ProcessedMessageEntity(eventId));
    }
}
