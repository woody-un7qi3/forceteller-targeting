package co.un7qi3.targeting.core.evaluation;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MapEvaluationInput implements EvaluationInput {

    private final String id;
    private final Map<String, Object> attributes;
    private final Instant evaluatedAt;

    public MapEvaluationInput(String id, Map<String, Object> attributes, Instant evaluatedAt) {
        this.id          = Objects.requireNonNull(id, "id");
        this.attributes  = Collections.unmodifiableMap(
            new LinkedHashMap<>(Objects.requireNonNull(attributes, "attributes")));
        this.evaluatedAt = Objects.requireNonNull(evaluatedAt, "evaluatedAt");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Instant evaluatedAt() {
        return evaluatedAt;
    }

    @Override
    public Optional<Object> attr(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(attributes.get(key));
    }
}
