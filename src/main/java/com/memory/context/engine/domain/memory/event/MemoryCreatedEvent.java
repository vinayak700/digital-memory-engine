package com.memory.context.engine.domain.memory.event;

import lombok.Getter;

/**
 * Event published when a new memory is created.
 */
@Getter
public class MemoryCreatedEvent extends MemoryDomainEvent {

    private final String title;
    private final int importanceScore;

    public MemoryCreatedEvent(Long memoryId, String userId, String title, int importanceScore) {
        super(memoryId, userId, "MEMORY_CREATED");
        this.title = title;
        this.importanceScore = importanceScore;
    }
}
