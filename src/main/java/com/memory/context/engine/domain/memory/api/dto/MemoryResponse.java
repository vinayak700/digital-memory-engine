package com.memory.context.engine.domain.memory.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryResponse {
    private Long id;
    private String title;
    private String content;
    private Map<String, Object> context;
    private int importanceScore;
    private boolean archived;
    private OffsetDateTime createdAt;
}
