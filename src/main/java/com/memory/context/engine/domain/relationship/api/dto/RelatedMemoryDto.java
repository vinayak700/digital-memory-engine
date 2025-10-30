package com.memory.context.engine.domain.relationship.api.dto;

import com.memory.context.engine.domain.relationship.entity.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO representing a related memory.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedMemoryDto {
    private Long memoryId;
    private String title;
    private RelationshipType relationshipType;
    private BigDecimal strength;
    private boolean isOutgoing;
}
