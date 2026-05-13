package co.un7qi3.targeting.core.rule;

import java.util.Map;

public record Rule(
    Long id,
    int version,
    RuleNode root,
    Map<String, Object> meta
) {
    public Rule {
        if (id == null || id <= 0)
            throw new IllegalArgumentException("Rule.id must be a positive Long");
        if (root == null)
            throw new IllegalArgumentException("Rule.root must not be null");
        if (version < 1)
            throw new IllegalArgumentException("Rule.version must be >= 1");
        meta = meta == null ? Map.of() : Map.copyOf(meta);
    }
}
