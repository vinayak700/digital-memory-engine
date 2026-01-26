package com.memory.context.engine.domain.intelligence.cache;

import lombok.Data;
import java.io.Serializable;

/**
 * Result of a semantic cache lookup.
 */
@Data
public class SemanticCacheResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean hit;
    private final String cachedValue;
    private final double similarityScore;
    private final String matchedQuestion;

    public static SemanticCacheResult miss() {
        return new SemanticCacheResult(false, null, 0.0, null);
    }

    public static SemanticCacheResult hit(String value, double similarity, String matchedQuestion) {
        return new SemanticCacheResult(true, value, similarity, matchedQuestion);
    }
}
