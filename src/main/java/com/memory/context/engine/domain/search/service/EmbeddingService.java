package com.memory.context.engine.domain.search.service;

import com.memory.context.engine.domain.memory.event.MemoryCreatedEvent;
import com.memory.context.engine.domain.memory.event.MemoryUpdatedEvent;
import com.memory.context.engine.infrastructure.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // Embedding dimension (OpenAI ada-002 uses 1536)
    private static final int EMBEDDING_DIMENSION = 1536;

    @KafkaListener(topics = KafkaConfig.Topics.MEMORY_EVENTS, groupId = "embedding-service-group")
    public void processEvent(Object event) {
        if (event instanceof MemoryCreatedEvent created) {
            generateEmbedding(created.getMemoryId());
        } else if (event instanceof MemoryUpdatedEvent updated) {
            if (updated.getUpdatedFields().contains("content") ||
                    updated.getUpdatedFields().contains("title")) {
                generateEmbedding(updated.getMemoryId());
            }
        }
    }

    /**
     * Generates and stores embedding for a memory.
     * 
     * TODO: Integrate with actual embedding API (OpenAI, Cohere, etc.)
     * For now, this is a placeholder that logs the operation.
     */
    public void generateEmbedding(Long memoryId) {
        log.info("Generating embedding for memory: {}", memoryId);

        // TODO: Actual implementation would:
        // 1. Fetch memory content from database
        // 2. Call embedding API (OpenAI, Cohere, etc.)
        // 3. Store the embedding vector

        // Placeholder - would be replaced with actual API call
        // float[] embedding = embeddingApiClient.generateEmbedding(content);
        // updateEmbedding(memoryId, embedding);

        log.debug("Embedding generation scheduled for memory: {}", memoryId);
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
