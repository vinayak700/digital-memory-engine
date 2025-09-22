package com.memory.context.engine.domain.memory.mapper;

import com.memory.context.engine.domain.memory.api.dto.MemoryResponse;
import com.memory.context.engine.domain.memory.entity.Memory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

/**
 * Maps between Memory entity and DTOs.
 * Extracted from service layer for Single Responsibility Principle.
 */
@Component
public class MemoryMapper {

        /**
         * Converts a Memory entity to its API response representation.
         *
         * @param memory the entity to convert
         * @return the response DTO
         */
        public MemoryResponse toResponse(Memory memory) {
                return MemoryResponse.builder()
                                .id(memory.getId())
                                .title(memory.getTitle())
                                .content(memory.getContent())
                                .context(memory.getContext())
                                .importanceScore(memory.getImportanceScore())
                                .archived(memory.isArchived())
                                .createdAt(memory.getCreatedAt() != null
                                                ? memory.getCreatedAt().atOffset(ZoneOffset.UTC)
                                                : null)
                                .updatedAt(memory.getUpdatedAt() != null
                                                ? memory.getUpdatedAt().atOffset(ZoneOffset.UTC)
                                                : null)
                                .build();
        }
}
