package com.memory.context.engine.domain.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.context.engine.domain.audit.entity.AuditEvent;
import com.memory.context.engine.domain.audit.repository.AuditRepository;
import com.memory.context.engine.domain.memory.event.MemoryDomainEvent;
import com.memory.context.engine.infrastructure.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit service that consumes events from Kafka and persists to audit store.
 * Provides compliance-ready audit trail.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.Topics.MEMORY_AUDIT, groupId = "audit-service-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void recordAuditEvent(MemoryDomainEvent event) {
        log.debug("Recording audit event: type={}, memoryId={}",
                event.getEventType(), event.getMemoryId());

        try {
            AuditEvent auditEvent = AuditEvent.builder()
                    .memoryId(event.getMemoryId())
                    .userId(event.getUserId())
                    .eventType(event.getEventType())
                    .eventData(serializeEventData(event))
                    .occurredAt(event.getOccurredAt())
                    .build();

            auditRepository.save(auditEvent);
            log.debug("Audit event recorded: id={}", auditEvent.getId());

        } catch (Exception e) {
            log.error("Failed to record audit event: type={}, memoryId={}",
                    event.getEventType(), event.getMemoryId(), e);
            // Don't rethrow - audit failures shouldn't break the flow
        }
    }

    private String serializeEventData(MemoryDomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize event data", e);
            return "{}";
        }
    }
}
