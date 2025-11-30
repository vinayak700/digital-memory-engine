package com.memory.context.engine.domain.search.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for semantic search.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder
public class SearchRequest {

    @NotBlank(message = "Query is required")
    private String query;

    @Min(1)
    @Max(50)
    @lombok.Builder.Default
    private int limit = 10;

    @lombok.Builder.Default
    private Double similarityThreshold = 0.7;
}
