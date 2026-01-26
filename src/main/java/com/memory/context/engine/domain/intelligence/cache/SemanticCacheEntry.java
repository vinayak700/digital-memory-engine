package com.memory.context.engine.domain.intelligence.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * Represents a cached entry with normalized keywords for semantic matching.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemanticCacheEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String originalQuestion;
    private Set<String> normalizedKeywords;
    private String cachedValue;
    private Instant createdAt;
    private double relevanceScore; // Optional: track how relevant the answer was

    public SemanticCacheEntry(String originalQuestion, Set<String> normalizedKeywords,
            String cachedValue, double relevanceScore) {
        this.originalQuestion = originalQuestion;
        this.normalizedKeywords = normalizedKeywords;
        this.cachedValue = cachedValue;
        this.createdAt = Instant.now();
        this.relevanceScore = relevanceScore;
    }
}
