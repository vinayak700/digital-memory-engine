package com.memory.context.engine.domain.common.exception;

public class AccessDeniedException extends DomainException {

    public AccessDeniedException(String message) {
        super("ACCESS_DENIED", message);
    }
}
