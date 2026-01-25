package com.memory.context.engine.domain.memory.event;

import lombok.Getter;

import java.util.Set;

/**
 * Event published when a memory is updated.
 */
@Getter
public class MemoryUpdatedEvent extends MemoryDomainEvent {

    private final Set<String> updatedFields;

    public MemoryUpdatedEvent(Long memoryId, String userId, Set<String> updatedFields) {
        super(memoryId, userId, "MEMORY_UPDATED");
        this.updatedFields = updatedFields;
    }
}
