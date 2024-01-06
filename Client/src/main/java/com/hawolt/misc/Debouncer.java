package com.hawolt.misc;

import java.util.concurrent.*;

/**
 * The Debouncer is used to prevent excessive execution of any Task,
 * this can be useful when an interaction is creating a lot of events.
 */
public class Debouncer {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<Object, Future<?>> delayedMap = new ConcurrentHashMap<>();

    /**
     * @param key      the key to differentiate between different tasks
     * @param runnable the task to execute
     * @param delay    delay until this task should execute,
     *                 given no other event with the same key occurs within the delay
     * @param unit     TimeUnit required for scheduling with the appropriate delay
     */
    public void debounce(final Object key, final Runnable runnable, long delay, TimeUnit unit) {
        final Future<?> prev = delayedMap.put(key, scheduler.schedule(() -> {
            try {
                runnable.run();
            } finally {
                delayedMap.remove(key);
            }
        }, delay, unit));
        if (prev != null) {
            prev.cancel(true);
        }
    }

    /**
     * shuts down the scheduler required to run the Debouncer.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}