package com.memory.context.engine.domain.intelligence;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to ask a question to the Answer Synthesis Engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AskRequest {

    @NotBlank(message = "Question is required")
    @Size(min = 3, max = 500, message = "Question must be 3-500 characters")
    private String question;

    /**
     * Whether to include related memories via graph expansion.
     */
    @Builder.Default
    @JsonSetter(nulls = Nulls.SKIP)
    private Boolean includeRelated = true;

    /**
     * Maximum number of source memories to use.
     */
    @Builder.Default
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer maxSources = 5;
}
