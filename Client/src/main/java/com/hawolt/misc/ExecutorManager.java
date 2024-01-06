package com.hawolt.misc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class to manage ExecutorService's within the Application
 */
public class ExecutorManager {
    private final static Map<String, ExecutorService> map = new HashMap<>();

    /**
     * obtains an ExecutorService instance associated with the provided name,
     * or creates a new one if none is registered.
     *
     * @param name the name for the ExecutorService that should be accessed
     * @param <T>  dynamic ExecutorService type
     * @return
     */
    public static <T> T getService(String name) {
        if (!map.containsKey(name)) map.put(name, Executors.newSingleThreadExecutor());
        return Unsafe.cast(map.get(name));
    }

    /**
     * obtains a ScheduledExecutorService instance associated with the provided name,
     * or creates a new one if none is registered.
     *
     * @param name the name for the ScheduledExecutorService that should be accessed
     * @param <T>  dynamic ExecutorService type
     * @return the ExecutorService associated with the provided name
     */
    public static <T> T getScheduledService(String name) {
        if (!map.containsKey(name)) map.put(name, Executors.newSingleThreadScheduledExecutor());
        return Unsafe.cast(map.get(name));
    }

    /**
     * registers an ExecutorService with the specified name
     *
     * @param name    the name associated with the ExecutorService
     * @param service the service to store
     * @param <T>     dynamic ExecutorService type
     * @return the specified service
     */
    public static <T> T registerService(String name, ExecutorService service) {
        map.put(name, service);
        return Unsafe.cast(service);
    }

    /**
     * utility to get access every registered ExecutorService
     *
     * @return a Collection of every registered ExecutorService
     */
    public static Collection<ExecutorService> get() {
        return map.values();
    }
}
