package com.hawolt.settings;

import com.hawolt.Unsafe;
import org.json.JSONObject;

import java.util.function.Supplier;

public class DynamicObject extends JSONObject {
    public DynamicObject(JSONObject o) {
        for (String key : o.keySet()) {
            put(key, o.get(key));
        }
    }

    public <T> T convert(Object o) {
        if (o == null) return null;
        return Unsafe.cast(o);
    }

    public <T> T getByKey(String key) {
        if (!has(key) || isNull(key)) return null;
        return convert(get(key));
    }

    public <T> T getByKeyOrDefault(String key, Object d) {
        return convert(has(key) ? get(key) : d);
    }

    public <T> T getByKeyOrDefaultNonNull(String key, Object d) {
        return convert(has(key) && !isNull(key) ? get(key) : d);
    }

    public <T> T getByKeyNonNullOrThrow(String key, Supplier<RuntimeException> supplier) {
        if (has(key) && !isNull(key)) {
            return convert(get(key));
        } else {
            throw supplier.get();
        }
    }
}
