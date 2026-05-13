package co.un7qi3.targeting.core.rule;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OperatorTest {

    @Test
    void eq_neq() {
        assertThat(Operator.EQ.apply("M", "M")).isTrue();
        assertThat(Operator.EQ.apply("M", "F")).isFalse();
        assertThat(Operator.NEQ.apply("M", "F")).isTrue();
    }

    @Test
    void compare() {
        assertThat(Operator.GTE.apply(20, 20)).isTrue();
        assertThat(Operator.GTE.apply(19, 20)).isFalse();
        assertThat(Operator.GT.apply(21, 20)).isTrue();
        assertThat(Operator.LT.apply(19, 20)).isTrue();
        assertThat(Operator.LTE.apply(20, 20)).isTrue();
    }

    @Test
    void in_notIn() {
        assertThat(Operator.IN.apply("KR", List.of("KR", "JP"))).isTrue();
        assertThat(Operator.IN.apply("US", List.of("KR", "JP"))).isFalse();
        assertThat(Operator.NOT_IN.apply("US", List.of("KR", "JP"))).isTrue();
    }

    @Test
    void between() {
        Map<String, Integer> range = Map.of("min", 20, "max", 29);
        assertThat(Operator.BETWEEN.apply(25, range)).isTrue();
        assertThat(Operator.BETWEEN.apply(20, range)).isTrue();
        assertThat(Operator.BETWEEN.apply(29, range)).isTrue();
        assertThat(Operator.BETWEEN.apply(30, range)).isFalse();
    }

    @Test
    void contains() {
        assertThat(Operator.CONTAINS.apply(List.of("welcome", "kr_20s"), "welcome")).isTrue();
        assertThat(Operator.CONTAINS.apply(List.of("kr_20s"), "welcome")).isFalse();
    }

    @Test
    void null_lhs_returns_false() {
        assertThat(Operator.EQ.apply(null, "M")).isFalse();
        assertThat(Operator.GTE.apply(null, 20)).isFalse();
        assertThat(Operator.IN.apply(null, List.of("KR"))).isFalse();
    }
}
