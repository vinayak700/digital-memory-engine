package com.memory.context.engine.domain.search.service;

import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;
import com.memory.context.engine.domain.memory.event.MemoryCreatedEvent;
import com.memory.context.engine.domain.memory.event.MemoryUpdatedEvent;
import com.memory.context.engine.domain.memory.event.MemoryDomainEvent;
import com.memory.context.engine.domain.search.event.EmbeddingGeneratedEvent;
import com.memory.context.engine.infrastructure.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service for generating and storing vector embeddings.
 * Listens to Kafka events for async processing.
 * 
 * NOTE: Actual embedding generation requires an external API (OpenAI, Cohere,
 * etc.)
 * This is a placeholder that would be extended with actual embedding calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final MemoryRepository memoryRepository;

    // Embedding dimension (OpenAI ada-002 uses 1536)
    private static final int EMBEDDING_DIMENSION = 1536;

    @KafkaListener(topics = KafkaConfig.Topics.MEMORY_EVENTS, groupId = "embedding-service-group", containerFactory = "kafkaListenerContainerFactory")
    public void processEvent(MemoryDomainEvent event) {
        if (event instanceof MemoryCreatedEvent created) {
            log.debug("Received Kafka MemoryCreatedEvent for memory: {}", created.getMemoryId());
            generateEmbedding(created.getMemoryId(), created.getUserId());
        } else if (event instanceof MemoryUpdatedEvent updated) {
            if (updated.getUpdatedFields().contains("content") || updated.getUpdatedFields().contains("title")) {
                log.debug("Received Kafka MemoryUpdatedEvent for memory: {}", updated.getMemoryId());
                generateEmbedding(updated.getMemoryId(), updated.getUserId());
            }
        }
    }

    /**
     * Generates and stores embedding for a memory.
     */
    public void generateEmbedding(Long memoryId, String userId) {
        log.info("Generating embedding for memory: {} for user: {}", memoryId, userId);

        Memory memory = memoryRepository.findById(memoryId).orElse(null);
        if (memory == null) {
            log.warn("Memory not found for embedding generation: {}", memoryId);
            return;
        }

        // Use word-based summation for deterministic "pseudo-embeddings"
        // This ensures shared words lead to higher vector similarity
        String[] words = (memory.getTitle() + " " + memory.getContent())
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .split("\\s+");

        float[] embedding = new float[EMBEDDING_DIMENSION];

        for (String word : words) {
            if (word.length() < 3)
                continue; // Skip small words

            java.util.Random random = new java.util.Random(word.hashCode());
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                embedding[i] += random.nextFloat() - 0.5f;
            }
        }

        // Normalize
        double norm = 0.0;
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            norm += embedding[i] * embedding[i];
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                embedding[i] /= (float) norm;
            }
        }

        updateEmbedding(memoryId, embedding);
        log.debug("Embedding generated and saved for memory: {}", memoryId);

        // Publish event for intelligent linking
        eventPublisher.publishEvent(new EmbeddingGeneratedEvent(memoryId, userId));
    }

    /**
     * Updates the embedding vector for a memory.
     */
    public void updateEmbedding(Long memoryId, float[] embedding) {
        if (embedding.length != EMBEDDING_DIMENSION) {
            log.error("Invalid embedding dimension: expected {}, got {}",
                    EMBEDDING_DIMENSION, embedding.length);
            return;
        }

        // Convert float array to pgvector format
        String vectorString = arrayToVectorString(embedding);

        jdbcTemplate.update(
                "UPDATE memories SET embedding = ?::vector WHERE id = ?",
                vectorString, memoryId);

        log.debug("Embedding updated for memory: {}", memoryId);
    }

    private String arrayToVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0)
                sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
