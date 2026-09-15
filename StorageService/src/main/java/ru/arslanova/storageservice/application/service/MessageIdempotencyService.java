package ru.arslanova.storageservice.application.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.arslanova.storageservice.infrastructure.entity.ProcessedMessageEntity;
import ru.arslanova.storageservice.infrastructure.repository.ProcessedMessageRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageIdempotencyService {
    private final ProcessedMessageRepository repository;

    public boolean alreadyProcessed(UUID eventId){
        return eventId != null && repository.existsByEventId(eventId);
    }

    @Transactional
    public void markProcessed(UUID eventId) {
        if (eventId == null){
            return;
        }
        repository.save(new ProcessedMessageEntity(eventId));
    }
}
