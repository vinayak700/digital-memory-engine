package com.memory.context.engine.domain.memory.event;

import com.memory.context.engine.infrastructure.kafka.MemoryEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Synchronous listener for memory domain events.
 * Logs events AND publishes to Kafka for async downstream processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryEventListener {

    private final MemoryEventProducer kafkaProducer;

    @EventListener
    public void handleMemoryCreated(MemoryCreatedEvent event) {
        log.info("Memory created: id={}, userId={}, title={}, importance={}",
                event.getMemoryId(),
                event.getUserId(),
                event.getTitle(),
                event.getImportanceScore());

        // Publish to Kafka for async processing
        kafkaProducer.publishEvent(event);
        kafkaProducer.publishAuditEvent(event);
    }

    @EventListener
    public void handleMemoryUpdated(MemoryUpdatedEvent event) {
        log.info("Memory updated: id={}, userId={}, fields={}",
                event.getMemoryId(),
                event.getUserId(),
                event.getUpdatedFields());

        // Publish to Kafka for async processing
        kafkaProducer.publishEvent(event);
        kafkaProducer.publishAuditEvent(event);
    }

    @EventListener
    public void handleMemoryArchived(MemoryArchivedEvent event) {
        log.info("Memory archived: id={}, userId={}",
                event.getMemoryId(),
                event.getUserId());

        // Publish to Kafka for async processing
        kafkaProducer.publishEvent(event);
        kafkaProducer.publishAuditEvent(event);
    }
}
