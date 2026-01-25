package com.memory.context.engine.domain.memory.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request DTO for updating an existing memory (PATCH semantics).
 * All fields are optional - only non-null values will be applied.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemoryRequest {

        @Size(min = 1, message = "Title cannot be empty if provided")
        private String title;

        @Size(min = 1, message = "Content cannot be empty if provided")
        private String content;

        @Min(value = 1, message = "Importance score must be between 1 and 10")
        @Max(value = 10, message = "Importance score must be between 1 and 10")
        private Integer importanceScore;

        private Map<String, Object> context;
}
