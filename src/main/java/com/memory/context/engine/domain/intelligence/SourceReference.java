package com.memory.context.engine.domain.intelligence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reference to a source memory used in answer generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceReference {

    /**
     * Memory ID.
     */
    private Long memoryId;

    /**
     * Memory title.
     */
    private String title;

    /**
     * Relevance score (0.0 to 1.0).
     */
    private double relevanceScore;
}
