package ru.arslanova.orderservice.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(name = "trace_id")
    private String traceId;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public OutboxEventEntity(
            UUID aggregateId,
            String eventType,
            String payload
    ) {
        this(aggregateId, eventType, payload, null);
    }

    public OutboxEventEntity(
            UUID aggregateId,
            String eventType,
            String payload,
            String traceId
    ){
        this.id = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.processed = false;
        this.createdAt = Instant.now();
        this.traceId = traceId;
        this.attempts = 0;
    }
}
