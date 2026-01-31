package com.memory.context.engine.domain.relationship.service;

import com.memory.context.engine.domain.memory.entity.Memory;
import com.memory.context.engine.domain.memory.repository.MemoryRepository;
import com.memory.context.engine.domain.relationship.entity.MemoryRelationship;
import com.memory.context.engine.domain.relationship.entity.RelationshipType;
import com.memory.context.engine.domain.relationship.repository.MemoryRelationshipRepository;
import com.memory.context.engine.domain.search.api.dto.SearchResult;
import com.memory.context.engine.domain.search.event.EmbeddingGeneratedEvent;
import com.memory.context.engine.domain.intelligence.GeminiService;
import com.memory.context.engine.domain.search.service.SearchService;
import com.memory.context.engine.infrastructure.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that automatically creates relationships between memories
 * based on semantic similarity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligentLinkingService {

    private final SearchService searchService;
    private final GeminiService geminiService;
    private final MemoryRepository memoryRepository;
    private final MemoryRelationshipRepository relationshipRepository;

    @Value("${intelligent.linking.threshold:0.85}")
    private double similarityThreshold;

    @Value("${intelligent.linking.max-links:3}")
    private int maxLinksPerMemory;

    @EventListener
    @Transactional
    @CacheEvict(value = CacheNames.GRAPH, allEntries = true)
    public void onEmbeddingGenerated(EmbeddingGeneratedEvent event) {
        Long memoryId = event.getMemoryId();
        String userId = event.getUserId();

        log.info("Intelligent linking triggered for memory: {} (User: {})", memoryId, userId);

        Memory sourceMemory = memoryRepository.findById(memoryId).orElse(null);
        if (sourceMemory == null || sourceMemory.isArchived()) {
            log.debug("Memory {} not found or is archived. Skipping intelligent linking.", memoryId);
            return;
        }

        // Find similar memories for the same user (lower threshold for candidates)
        List<SearchResult> similarResults = searchService.findSimilarForUser(memoryId, userId, maxLinksPerMemory + 5);

        int linksCreated = 0;
        for (SearchResult result : similarResults) {
            if (linksCreated >= maxLinksPerMemory)
                break;

            // Skip self-linking
            if (result.getId().equals(memoryId))
                continue;

            // Skip if similarity is very low (initial pre-filter for performance)
            if (result.getSimilarityScore() < 0.3)
                continue;

            // Use AI to decide
            Memory targetMemory = memoryRepository.findById(result.getId()).orElse(null);
            if (targetMemory != null && !targetMemory.isArchived()
                    && geminiService.shouldLink(sourceMemory, targetMemory)) {

                // Check if relationship already exists in EITHER direction
                boolean alreadyLinked = relationshipRepository
                        .existsBySourceMemoryIdAndTargetMemoryIdAndRelationshipType(
                                memoryId, result.getId(), RelationshipType.AUTO_LINKED)
                        ||
                        relationshipRepository.existsBySourceMemoryIdAndTargetMemoryIdAndRelationshipType(
                                result.getId(), memoryId, RelationshipType.AUTO_LINKED);

                if (!alreadyLinked) {
                    MemoryRelationship relationship = MemoryRelationship.builder()
                            .sourceMemory(sourceMemory)
                            .targetMemory(targetMemory)
                            .relationshipType(RelationshipType.AUTO_LINKED)
                            .strength(java.math.BigDecimal.valueOf(result.getSimilarityScore()))
                            .build();
                    relationshipRepository.save(relationship);
                    log.info("AI-Linked memory {} to {} (Similarity: {})", memoryId, result.getId(),
                            result.getSimilarityScore());

                    linksCreated++;
                }
            }
        }

    }
}
