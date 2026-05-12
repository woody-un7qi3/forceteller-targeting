package co.un7qi3.targeting.core.attribute;

import co.un7qi3.targeting.core.rule.Operator;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributeSpecTest {

    @Test
    void creates_with_required_fields() {
        var spec = new AttributeSpec(
            "user.age", AttributeType.INTEGER,
            EnumSet.of(Operator.GTE, Operator.LTE, Operator.BETWEEN),
            "나이", "한국 나이", AttributeStatus.ACTIVE
        );

        assertThat(spec.key()).isEqualTo("user.age");
        assertThat(spec.type()).isEqualTo(AttributeType.INTEGER);
        assertThat(spec.allows(Operator.GTE)).isTrue();
        assertThat(spec.allows(Operator.IN)).isFalse();
        assertThat(spec.status()).isEqualTo(AttributeStatus.ACTIVE);
    }

    @Test
    void defaults_status_to_active_when_null() {
        var spec = new AttributeSpec(
            "user.age", AttributeType.INTEGER,
            EnumSet.of(Operator.GTE),
            "나이", null, null
        );
        assertThat(spec.status()).isEqualTo(AttributeStatus.ACTIVE);
    }

    @Test
    void rejects_blank_key() {
        assertThatThrownBy(() -> new AttributeSpec(
            "", AttributeType.STRING, EnumSet.of(Operator.EQ), null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_empty_allowed_ops() {
        assertThatThrownBy(() -> new AttributeSpec(
            "k", AttributeType.STRING, Set.of(), null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowed_ops_is_immutable_copy() {
        var ops = EnumSet.of(Operator.EQ, Operator.NEQ);
        var spec = new AttributeSpec(
            "k", AttributeType.STRING, ops, null, null, null);

        ops.remove(Operator.EQ);  // 원본 변경

        assertThat(spec.allowedOps()).containsExactlyInAnyOrder(Operator.EQ, Operator.NEQ);
    }

    // ──── of() 정적 팩토리 — ops 명시 ────

    @Test
    void of_with_explicit_ops() {
        var spec = AttributeSpec.of("user.age", AttributeType.INTEGER,
            EnumSet.of(Operator.GTE, Operator.LTE), "만 나이");
        assertThat(spec.allowedOps()).containsExactlyInAnyOrder(Operator.GTE, Operator.LTE);
        assertThat(spec.label()).isEqualTo("만 나이");
    }

    // ──── of() 정적 팩토리 — ops 생략 = 타입 기본 ────

    @Test
    void of_boolean_default_ops() {
        var spec = AttributeSpec.of("user.isAdmin", AttributeType.BOOLEAN, "어드민");
        assertThat(spec.allowedOps()).containsExactlyInAnyOrder(Operator.EQ, Operator.NEQ);
    }

    @Test
    void of_string_default_ops() {
        var spec = AttributeSpec.of("user.gender", AttributeType.STRING, "성별");
        assertThat(spec.allowedOps()).containsExactlyInAnyOrder(
            Operator.EQ, Operator.NEQ, Operator.IN, Operator.NOT_IN);
    }

    @Test
    void of_integer_default_ops() {
        var spec = AttributeSpec.of("user.age", AttributeType.INTEGER, "나이");
        assertThat(spec.allowedOps()).containsExactlyInAnyOrder(
            Operator.EQ, Operator.NEQ, Operator.GT, Operator.GTE,
            Operator.LT, Operator.LTE, Operator.BETWEEN);
    }

    @Test
    void of_instant_default_ops() {
        var spec = AttributeSpec.of("user.signupAt", AttributeType.INSTANT, "가입 시각");
        assertThat(spec.allowedOps()).containsExactlyInAnyOrder(
            Operator.GT, Operator.GTE, Operator.LT, Operator.LTE, Operator.BETWEEN);
        assertThat(spec.allows(Operator.EQ)).isFalse();
    }

    @Test
    void of_list_string_default_ops() {
        var spec = AttributeSpec.of("user.segments", AttributeType.LIST_STRING, "세그먼트");
        assertThat(spec.allowedOps()).containsExactly(Operator.CONTAINS);
    }
}
