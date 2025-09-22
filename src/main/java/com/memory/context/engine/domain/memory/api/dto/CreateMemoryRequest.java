package com.memory.context.engine.domain.memory.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder
public class CreateMemoryRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "content is required")
    private String content;

    @Min(value = 1, message = "importanceScore must be >= 1")
    @Max(value = 10, message = "importanceScore must be <= 10")
    private int importanceScore;

    // Flexible context payload
    private Map<String, Object> context;
}
