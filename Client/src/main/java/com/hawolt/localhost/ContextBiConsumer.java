package com.hawolt.localhost;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public record ContextBiConsumer<T>(T element, BiConsumer<Context, T> consumer) implements Handler {
    @Override
    public void handle(@NotNull Context context) throws Exception {
        this.consumer.accept(context, element);
    }
}