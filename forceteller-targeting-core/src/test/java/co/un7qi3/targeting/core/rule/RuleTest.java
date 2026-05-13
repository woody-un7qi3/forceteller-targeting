package co.un7qi3.targeting.core.rule;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleTest {

    private RuleNode sampleRoot() {
        return new RuleNode.And("n0", List.of(
            new RuleNode.Compare("n1", "user.age", Operator.GTE, 20)
        ));
    }

    @Test
    void creates_with_required_fields() {
        Rule rule = new Rule(1L, 1, sampleRoot(), null);
        assertThat(rule.id()).isEqualTo(1L);
        assertThat(rule.version()).isEqualTo(1);
        assertThat(rule.root()).isNotNull();
        assertThat(rule.meta()).isEmpty();
    }

    @Test
    void rejects_invalid_id() {
        assertThatThrownBy(() -> new Rule(null, 1, sampleRoot(), null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule(0L, 1, sampleRoot(), null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule(-1L, 1, sampleRoot(), null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_invalid_version() {
        assertThatThrownBy(() -> new Rule(1L, 0, sampleRoot(), null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule(1L, -1, sampleRoot(), null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_null_root() {
        assertThatThrownBy(() -> new Rule(1L, 1, null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void meta_is_immutable_copy() {
        Map<String, Object> mutable = new HashMap<>();
        mutable.put("label", "first");

        Rule rule = new Rule(1L, 1, sampleRoot(), mutable);

        // 원본을 변경해도 rule.meta에는 영향 없어야 함
        mutable.put("label", "changed");
        assertThat(rule.meta().get("label")).isEqualTo("first");

        // 반환된 meta도 unmodifiable
        assertThatThrownBy(() -> rule.meta().put("x", "y"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
