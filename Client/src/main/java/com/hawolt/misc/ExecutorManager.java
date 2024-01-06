package com.hawolt;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorManager {
    private final static Map<String, ExecutorService> map = new HashMap<>();

    public static <T> T getService(String name) {
        if (!map.containsKey(name)) map.put(name, Executors.newSingleThreadExecutor());
        return Unsafe.cast(map.get(name));
    }

    public static <T> T getScheduledService(String name) {
        if (!map.containsKey(name)) map.put(name, Executors.newSingleThreadScheduledExecutor());
        return Unsafe.cast(map.get(name));
    }

    public static <T> T registerService(String name, ExecutorService service) {
        map.put(name, service);
        return Unsafe.cast(service);
    }

    public static Collection<ExecutorService> get() {
        return map.values();
    }
}
