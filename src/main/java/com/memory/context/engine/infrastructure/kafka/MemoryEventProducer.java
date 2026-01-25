package com.memory.context.engine.infrastructure.kafka;

import com.memory.context.engine.domain.memory.event.MemoryDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka producer for memory domain events.
 * Publishes events to Kafka topics for async processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a memory domain event to Kafka.
     *
     * @param event the domain event to publish
     */
    public void publishEvent(MemoryDomainEvent event) {
        String key = String.valueOf(event.getMemoryId());

        kafkaTemplate.send(KafkaConfig.Topics.MEMORY_EVENTS, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event: type={}, memoryId={}",
                                event.getEventType(), event.getMemoryId(), ex);
                    } else {
                        log.debug("Event published: type={}, memoryId={}, partition={}, offset={}",
                                event.getEventType(),
                                event.getMemoryId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Publishes an event to the audit topic.
     */
    public void publishAuditEvent(MemoryDomainEvent event) {
        String key = String.valueOf(event.getMemoryId());

        kafkaTemplate.send(KafkaConfig.Topics.MEMORY_AUDIT, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish audit event: type={}, memoryId={}",
                                event.getEventType(), event.getMemoryId(), ex);
                    }
                });
    }
}
