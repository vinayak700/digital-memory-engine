package com.memory.context.engine.domain.common.exception;

public class InvalidMemoryStateException extends DomainException {

    public InvalidMemoryStateException(String message) {
        super("INVALID_MEMORY_STATE", message);
    }
}
