package com.hawolt;

public class Unsafe {
    /**
     * Used to convert any Object reference to the required type.
     * Invoking this method to convert an Object to an incompatible type will throw an Exception.
     *
     * @param o   the Object that should be converted to the required type by the JVM
     * @param <T> dynamically decided by the JVM
     * @return the Object as an Instance of type T
     */
    @SuppressWarnings(value = "all")
    public static <T> T cast(Object o) {
        return (T) o;
    }
}