package com.memory.context.engine.infrastructure.api.error;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ApiErrorResponse {
    private String code;
    private String message;
    private Instant timestamp;
    private String path;
}
