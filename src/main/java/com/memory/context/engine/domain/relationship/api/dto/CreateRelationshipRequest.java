package com.memory.context.engine.domain.relationship.api.dto;

import com.memory.context.engine.domain.relationship.entity.RelationshipType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a memory relationship.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRelationshipRequest {

    @NotNull
    private Long sourceMemoryId;

    @NotNull
    private Long targetMemoryId;

    @NotNull
    private RelationshipType type;

    private BigDecimal strength = BigDecimal.ONE;
}
