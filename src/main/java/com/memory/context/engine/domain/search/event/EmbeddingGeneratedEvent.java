package com.memory.context.engine.domain.search.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Event published when a vector embedding has been generated for a memory.
 */
@Getter
@AllArgsConstructor
public class EmbeddingGeneratedEvent {
    private final Long memoryId;
    private final String userId;
}
