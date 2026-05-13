package co.un7qi3.targeting.core.rule;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleNodesTest {

    @Test
    void single_compare() {
        var root = new RuleNode.Compare("c1", "user.isAdmin", Operator.EQ, true);
        assertThat(RuleNodes.usedAttributes(root)).containsExactly("user.isAdmin");
    }

    @Test
    void and_collects_all_children() {
        var root = new RuleNode.And("n0", List.of(
            new RuleNode.Compare("c1", "user.isAdmin", Operator.EQ, true),
            new RuleNode.Compare("c2", "now.kstHour", Operator.GTE, 18)
        ));
        assertThat(RuleNodes.usedAttributes(root))
            .containsExactlyInAnyOrder("user.isAdmin", "now.kstHour");
    }

    @Test
    void nested_or_and_not() {
        var root = new RuleNode.And("n0", List.of(
            new RuleNode.Or("n1", List.of(
                new RuleNode.Compare("c1", "user.isAdmin", Operator.EQ, true),
                new RuleNode.Compare("c2", "user.isVip", Operator.EQ, true)
            )),
            new RuleNode.Not("n2",
                new RuleNode.Compare("c3", "now.kstHour", Operator.GTE, 18))
        ));
        assertThat(RuleNodes.usedAttributes(root))
            .containsExactlyInAnyOrder("user.isAdmin", "user.isVip", "now.kstHour");
    }

    @Test
    void duplicate_keys_deduplicated() {
        var root = new RuleNode.And("n0", List.of(
            new RuleNode.Compare("c1", "user.age", Operator.GTE, 20),
            new RuleNode.Compare("c2", "user.age", Operator.LTE, 30)
        ));
        assertThat(RuleNodes.usedAttributes(root)).containsExactly("user.age");
    }
}
