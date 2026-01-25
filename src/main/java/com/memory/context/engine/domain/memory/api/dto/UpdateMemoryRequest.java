package com.memory.context.engine.domain.memory.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemoryRequest {

        @NotBlank(message = "Title must not be blank")
        private String title;

        @NotBlank(message = "Content must not be blank")
        private String content;

        @Min(value = 1, message = "Importance score must be between 1 and 10")
        @Max(value = 10, message = "Importance score must be between 1 and 10")
        private Integer importanceScore;

        private Map<String, Object> context;
}
