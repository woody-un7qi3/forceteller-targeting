package co.un7qi3.targeting.core.attribute;

import co.un7qi3.targeting.core.rule.Operator;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record AttributeSpec(
    String key,
    AttributeType type,
    Set<Operator> allowedOps,
    String label,
    String description,
    AttributeStatus status
) {
    public AttributeSpec {
        Objects.requireNonNull(key, "key");
        if (key.isBlank())
            throw new IllegalArgumentException("AttributeSpec.key must not be blank");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(allowedOps, "allowedOps");
        if (allowedOps.isEmpty())
            throw new IllegalArgumentException("AttributeSpec.allowedOps must not be empty");
        allowedOps = EnumSet.copyOf(allowedOps);
        if (status == null) status = AttributeStatus.ACTIVE;
    }

    public boolean allows(Operator op) {
        return allowedOps.contains(op);
    }

    // ──── 정적 팩토리 ────

    /** label/description 없이. status는 ACTIVE 기본. */
    public static AttributeSpec of(String key, AttributeType type, Set<Operator> allowedOps) {
        return new AttributeSpec(key, type, allowedOps, null, null, AttributeStatus.ACTIVE);
    }

    /** label 포함. description 없이. status는 ACTIVE 기본. */
    public static AttributeSpec of(String key, AttributeType type, Set<Operator> allowedOps, String label) {
        return new AttributeSpec(key, type, allowedOps, label, null, AttributeStatus.ACTIVE);
    }

    /** label + description 포함. status는 ACTIVE 기본. */
    public static AttributeSpec of(String key, AttributeType type, Set<Operator> allowedOps,
                                   String label, String description) {
        return new AttributeSpec(key, type, allowedOps, label, description, AttributeStatus.ACTIVE);
    }

    // ──── 타입 기본 ops 자동 매핑 (ops 인자 생략 시 사용) ────

    /** ops 생략 → 타입에 자연스러운 기본 ops 자동. label만 지정. */
    public static AttributeSpec of(String key, AttributeType type, String label) {
        return new AttributeSpec(key, type, defaultOpsFor(type), label, null, AttributeStatus.ACTIVE);
    }

    /** ops 생략 → 타입에 자연스러운 기본 ops 자동. label + description. */
    public static AttributeSpec of(String key, AttributeType type, String label, String description) {
        return new AttributeSpec(key, type, defaultOpsFor(type), label, description, AttributeStatus.ACTIVE);
    }

    /** 타입에 자연스러운 기본 op 집합. */
    public static Set<Operator> defaultOpsFor(AttributeType type) {
        return switch (type) {
            case BOOLEAN     -> EnumSet.of(Operator.EQ, Operator.NEQ);
            case STRING      -> EnumSet.of(Operator.EQ, Operator.NEQ, Operator.IN, Operator.NOT_IN);
            case INTEGER     -> EnumSet.of(Operator.EQ, Operator.NEQ, Operator.GT, Operator.GTE,
                                           Operator.LT, Operator.LTE, Operator.BETWEEN);
            case INSTANT     -> EnumSet.of(Operator.GT, Operator.GTE, Operator.LT, Operator.LTE, Operator.BETWEEN);
            case LIST_STRING -> EnumSet.of(Operator.CONTAINS);
        };
    }
}
