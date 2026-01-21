package com.memory.context.engine.domain.memory.exception;

public class MemoryNotFoundException extends RuntimeException {
    public MemoryNotFoundException(Long id) {
        super("Memory not found with id: " + id);
    }
}