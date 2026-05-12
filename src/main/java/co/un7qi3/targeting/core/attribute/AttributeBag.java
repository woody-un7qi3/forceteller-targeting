package co.un7qi3.targeting.core.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AttributeBag {

    private final Map<String, Object> values = new LinkedHashMap<>();

    public AttributeBag put(String key, Object value) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank())
            throw new IllegalArgumentException("attribute key must not be blank");
        values.put(key, value);
        return this;
    }

    public AttributeBag putAll(Map<String, ?> entries) {
        entries.forEach(this::put);
        return this;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Object get(String key) {
        return values.get(key);
    }

    public Collection<String> keys() {
        return values.keySet();
    }

    public Map<String, Object> snapshot() {
        // Map.copyOf는 null value를 허용하지 않으므로 LinkedHashMap + unmodifiable 사용.
        // 순서 보존 + null value 허용.
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public int size() {
        return values.size();
    }
}
