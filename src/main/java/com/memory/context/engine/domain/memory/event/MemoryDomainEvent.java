package com.memory.context.engine.domain.memory.event;

import lombok.Getter;

import java.time.Instant;

/**
 * Base class for all memory domain events.
 * Provides common fields for event tracking and auditing.
 */
@Getter
public abstract class MemoryDomainEvent {

    private final Long memoryId;
    private final String userId;
    private final Instant occurredAt;
    private final String eventType;

    protected MemoryDomainEvent(Long memoryId, String userId, String eventType) {
        this.memoryId = memoryId;
        this.userId = userId;
        this.occurredAt = Instant.now();
        this.eventType = eventType;
    }
}
