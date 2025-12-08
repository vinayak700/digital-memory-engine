package com.memory.context.engine.domain.memory.event;

/**
 * Event published when a memory is archived.
 */
@lombok.NoArgsConstructor
public class MemoryArchivedEvent extends MemoryDomainEvent {

    public MemoryArchivedEvent(Long memoryId, String userId) {
        super(memoryId, userId, "MEMORY_ARCHIVED");
    }
}
