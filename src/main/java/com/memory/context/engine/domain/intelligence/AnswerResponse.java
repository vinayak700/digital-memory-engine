package com.memory.context.engine.domain.intelligence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from the Answer Synthesis Engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {

    /**
     * The original question asked.
     */
    private String question;

    /**
     * The synthesized answer from memories.
     */
    private String answer;

    /**
     * Confidence score (0.0 to 1.0).
     */
    private double confidence;

    /**
     * Sources used to generate the answer.
     */
    private List<SourceReference> sources;

    /**
     * IDs of related memories found via graph expansion.
     */
    private List<Long> relatedMemories;
}
