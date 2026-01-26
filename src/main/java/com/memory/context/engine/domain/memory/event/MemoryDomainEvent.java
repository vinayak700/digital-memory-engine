package com.memory.context.engine.domain.memory.event;

import lombok.Getter;

import java.time.Instant;

/**
 * Base class for all memory domain events.
 * Provides common fields for event tracking and auditing.
 */
@Getter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public abstract class MemoryDomainEvent {

    private Long memoryId;
    private String userId;
    private Instant occurredAt;
    private String eventType;

    protected MemoryDomainEvent(Long memoryId, String userId, String eventType) {
        this.memoryId = memoryId;
        this.userId = userId;
        this.occurredAt = Instant.now();
        this.eventType = eventType;
    }
}
