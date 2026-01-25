package com.memory.context.engine.domain.common.util;

import lombok.NoArgsConstructor;

import java.util.function.Consumer;

@NoArgsConstructor
public final class MemoryUtils {
    public static <T> void setIfNotNull(T newValue, Consumer<T> setter) {
        if (newValue != null) {
            setter.accept(newValue);
        }
    }
}
