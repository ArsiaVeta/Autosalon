package ru.arslanova.storageservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.arslanova.storageservice.application.service.MessageIdempotencyService;
import ru.arslanova.storageservice.infrastructure.entity.ProcessedMessageEntity;
import ru.arslanova.storageservice.infrastructure.repository.ProcessedMessageRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageIdempotencyServiceTest {

    @Mock
    ProcessedMessageRepository repository;
    @InjectMocks
    MessageIdempotencyService service;

    @Test
    void alreadyProcessedReturnsFalseForNullEventId() {
        assertThat(service.alreadyProcessed(null)).isFalse();
    }

    @Test
    void alreadyProcessedQueriesRepositoryForKnownId() {
        UUID id = UUID.randomUUID();
        when(repository.existsByEventId(id)).thenReturn(true);

        assertThat(service.alreadyProcessed(id)).isTrue();
        verify(repository).existsByEventId(id);
    }

    @Test
    void markProcessedSavesEntity() {
        UUID id = UUID.randomUUID();
        service.markProcessed(id);
        verify(repository).save(any(ProcessedMessageEntity.class));
    }

    @Test
    void markProcessedSkipsNullId() {
        service.markProcessed(null);
        verify(repository, never()).save(any());
    }
}
