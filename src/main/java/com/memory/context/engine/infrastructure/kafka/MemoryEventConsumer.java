package com.memory.context.engine.infrastructure.kafka;

import com.memory.context.engine.domain.memory.event.MemoryArchivedEvent;
import com.memory.context.engine.domain.memory.event.MemoryCreatedEvent;
import com.memory.context.engine.domain.memory.event.MemoryDomainEvent;
import com.memory.context.engine.domain.memory.event.MemoryUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for memory domain events.
 * Processes events asynchronously for downstream operations.
 */
@Slf4j
@Component
public class MemoryEventConsumer {

    @KafkaListener(topics = KafkaConfig.Topics.MEMORY_EVENTS, groupId = "${spring.kafka.consumer.group-id:memory-engine-group}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeEvent(MemoryDomainEvent event) {
        log.info("Consuming event: type={}, memoryId={}, userId={}",
                event.getEventType(),
                event.getMemoryId(),
                event.getUserId());

        // Java 17 compatible - use instanceof instead of pattern matching switch
        if (event instanceof MemoryCreatedEvent created) {
            handleCreated(created);
        } else if (event instanceof MemoryUpdatedEvent updated) {
            handleUpdated(updated);
        } else if (event instanceof MemoryArchivedEvent archived) {
            handleArchived(archived);
        } else {
            log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void handleCreated(MemoryCreatedEvent event) {
        log.debug("Processing memory created: id={}, title={}",
                event.getMemoryId(), event.getTitle());
        // Future: trigger vector embedding generation
    }

    private void handleUpdated(MemoryUpdatedEvent event) {
        log.debug("Processing memory updated: id={}, fields={}",
                event.getMemoryId(), event.getUpdatedFields());
        // Future: update vector embeddings if content changed
    }

    private void handleArchived(MemoryArchivedEvent event) {
        log.debug("Processing memory archived: id={}",
                event.getMemoryId());
        // Future: update search index
    }
}
