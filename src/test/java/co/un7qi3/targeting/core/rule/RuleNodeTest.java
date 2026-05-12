package co.un7qi3.targeting.core.rule;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleNodeTest {

    @Test
    void and_creates_with_children() {
        var n = new RuleNode.And("n0", List.of(
            new RuleNode.Compare("c1", "user.age", Operator.GTE, 20)
        ));
        assertThat(n.id()).isEqualTo("n0");
        assertThat(n.nodes()).hasSize(1);
    }

    @Test
    void and_empty_children_is_allowed() {
        var n = new RuleNode.And("n0", List.of());
        assertThat(n.nodes()).isEmpty();
    }

    @Test
    void and_null_children_becomes_empty() {
        var n = new RuleNode.And("n0", null);
        assertThat(n.nodes()).isEmpty();
    }

    @Test
    void and_children_list_is_immutable_copy() {
        var children = new ArrayList<RuleNode>();
        children.add(new RuleNode.Compare("c1", "user.age", Operator.GTE, 20));

        var n = new RuleNode.And("n0", children);
        children.clear();   // 원본 변경

        assertThat(n.nodes()).hasSize(1);   // record는 영향 받지 않음
        assertThatThrownBy(() -> n.nodes().add(null))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void or_works_like_and() {
        var n = new RuleNode.Or("n0", List.of(
            new RuleNode.Compare("c1", "user.country", Operator.EQ, "KR")
        ));
        assertThat(n.id()).isEqualTo("n0");
        assertThat(n.nodes()).hasSize(1);
    }

    @Test
    void not_requires_single_child() {
        var inner = new RuleNode.Compare("c1", "event.flag", Operator.EQ, true);
        var n = new RuleNode.Not("n0", inner);
        assertThat(n.node()).isEqualTo(inner);
    }

    @Test
    void not_rejects_null_child() {
        assertThatThrownBy(() -> new RuleNode.Not("n0", null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compare_validates_required_fields() {
        // 정상
        new RuleNode.Compare("c1", "user.age", Operator.GTE, 20);

        // id 빈값
        assertThatThrownBy(() -> new RuleNode.Compare("", "user.age", Operator.GTE, 20))
            .isInstanceOf(IllegalArgumentException.class);

        // attribute 빈값
        assertThatThrownBy(() -> new RuleNode.Compare("c1", "", Operator.GTE, 20))
            .isInstanceOf(IllegalArgumentException.class);

        // op null
        assertThatThrownBy(() -> new RuleNode.Compare("c1", "user.age", null, 20))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compare_allows_null_value() {
        // null value는 허용 (EQ null 같은 비교 가능)
        var n = new RuleNode.Compare("c1", "user.age", Operator.EQ, null);
        assertThat(n.value()).isNull();
    }

    @Test
    void rejects_blank_ids() {
        assertThatThrownBy(() -> new RuleNode.And("", List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleNode.Or(null, List.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleNode.Not("  ",
                new RuleNode.Compare("c1", "x", Operator.EQ, 1)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
