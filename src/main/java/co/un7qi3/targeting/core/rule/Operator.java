package co.un7qi3.targeting.core.rule;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public enum Operator {
    EQ, NEQ,
    GT, GTE, LT, LTE,
    IN, NOT_IN,
    BETWEEN,
    CONTAINS;

    /**
     * lhs(속성값)와 rhs(룰 값)을 비교한다.
     *
     * @param lhs EvaluationInput에서 꺼낸 속성값. null 가능.
     * @param rhs 룰의 우변값. 스칼라/Collection/Map{min,max}.
     */
    public boolean apply(Object lhs, Object rhs) {
        return switch (this) {
            case EQ      -> Objects.equals(lhs, rhs);
            case NEQ     -> !Objects.equals(lhs, rhs);
            case GT      -> compare(lhs, rhs) >  0;
            case GTE     -> compare(lhs, rhs) >= 0;
            case LT      -> compare(lhs, rhs) <  0;
            case LTE     -> compare(lhs, rhs) <= 0;
            case IN      -> lhs != null && asCollection(rhs).contains(lhs);
            case NOT_IN  -> lhs == null || !asCollection(rhs).contains(lhs);
            case BETWEEN -> {
                Map<?, ?> range = asMap(rhs);
                yield compare(lhs, range.get("min")) >= 0
                   && compare(lhs, range.get("max")) <= 0;
            }
            case CONTAINS -> lhs != null && asCollection(lhs).contains(rhs);
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compare(Object lhs, Object rhs) {
        if (lhs == null || rhs == null) return Integer.MIN_VALUE;
        if (!(lhs instanceof Comparable) || !(rhs instanceof Comparable)) {
            throw new IllegalArgumentException("not comparable: " + lhs + " / " + rhs);
        }
        return ((Comparable) lhs).compareTo(rhs);
    }

    private static Collection<?> asCollection(Object v) {
        if (v instanceof Collection<?> c) return c;
        throw new IllegalArgumentException("expected Collection, got: " + v);
    }

    private static Map<?, ?> asMap(Object v) {
        if (v instanceof Map<?, ?> m) return m;
        throw new IllegalArgumentException("expected Map{min,max}, got: " + v);
    }
}
