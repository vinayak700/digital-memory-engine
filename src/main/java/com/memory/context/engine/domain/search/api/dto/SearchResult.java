package com.memory.context.engine.domain.search.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search result DTO with memory info and similarity score.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {
    private Long id;
    private String title;
    private String contentSnippet;
    private double similarityScore;
}
