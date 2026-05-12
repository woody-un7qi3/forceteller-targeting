package co.un7qi3.targeting.engine.validate;

import co.un7qi3.targeting.core.attribute.AttributeCatalog;
import co.un7qi3.targeting.core.attribute.AttributeProvider;
import co.un7qi3.targeting.core.attribute.AttributeSpec;
import co.un7qi3.targeting.core.attribute.AttributeStatus;
import co.un7qi3.targeting.core.attribute.AttributeBag;
import co.un7qi3.targeting.core.attribute.AttributeType;
import co.un7qi3.targeting.core.error.RuleValidationException;
import co.un7qi3.targeting.core.rule.Operator;
import co.un7qi3.targeting.core.rule.Rule;
import co.un7qi3.targeting.core.rule.RuleNode;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleValidatorTest {

    static class FakeProvider implements AttributeProvider<Object> {
        private final String ns;
        private final List<AttributeSpec> specs;
        FakeProvider(String ns, List<AttributeSpec> specs) { this.ns = ns; this.specs = specs; }
        @Override public String namespace() { return ns; }
        @Override public List<AttributeSpec> declare() { return specs; }
        @Override public void fill(Object subject, AttributeBag bag, java.util.Set<String> neededKeys) { }
    }

    private static AttributeSpec spec(String key, AttributeType type, AttributeStatus status, Operator... ops) {
        return new AttributeSpec(key, type, EnumSet.copyOf(List.of(ops)), null, null, status);
    }

    private final AttributeCatalog catalog = AttributeCatalog.of(List.of(
        new FakeProvider("user", List.of(
            spec("user.age", AttributeType.INTEGER, AttributeStatus.ACTIVE, Operator.GTE, Operator.LTE, Operator.BETWEEN),
            spec("user.gender", AttributeType.STRING, AttributeStatus.ACTIVE, Operator.EQ, Operator.IN),
            spec("user.legacy", AttributeType.STRING, AttributeStatus.DEPRECATED, Operator.EQ)
        ))
    ));

    private final RuleValidator validator = new RuleValidator(catalog);

    private Rule rule(RuleNode root) {
        return new Rule(1L, 1, root, null);
    }

    @Test
    void valid_rule_passes() {
        var report = validator.validate(rule(new RuleNode.And("n0", List.of(
            new RuleNode.Compare("c1", "user.age", Operator.GTE, 20),
            new RuleNode.Compare("c2", "user.gender", Operator.EQ, "M")
        ))));
        assertThat(report.ok()).isTrue();
        assertThat(report.warnings()).isEmpty();
    }

    @Test
    void unknown_attribute_is_error() {
        assertThatThrownBy(() -> validator.validate(rule(
            new RuleNode.Compare("c1", "user.unknown", Operator.EQ, "x"))))
            .isInstanceOf(RuleValidationException.class)
            .hasMessageContaining("unknown attribute");
    }

    @Test
    void disallowed_operator_is_error() {
        // user.age는 GTE/LTE/BETWEEN만 허용. IN은 불허
        assertThatThrownBy(() -> validator.validate(rule(
            new RuleNode.Compare("c1", "user.age", Operator.IN, List.of(20, 30)))))
            .isInstanceOf(RuleValidationException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    void duplicate_node_id_is_error() {
        assertThatThrownBy(() -> validator.validate(rule(new RuleNode.And("n0", List.of(
            new RuleNode.Compare("dup", "user.age", Operator.GTE, 20),
            new RuleNode.Compare("dup", "user.gender", Operator.EQ, "M")
        )))))
            .isInstanceOf(RuleValidationException.class)
            .hasMessageContaining("duplicate node id");
    }

    @Test
    void deprecated_attribute_is_warning_not_error() {
        var report = validator.validate(rule(
            new RuleNode.Compare("c1", "user.legacy", Operator.EQ, "x")));
        assertThat(report.ok()).isTrue();
        assertThat(report.warnings()).anyMatch(w -> w.contains("DEPRECATED"));
    }

    @Test
    void max_depth_is_enforced() {
        // 깊이 3 트리, max=2로 강제
        var deep = new RuleNode.And("a", List.of(
            new RuleNode.And("b", List.of(
                new RuleNode.And("c", List.of(
                    new RuleNode.Compare("d", "user.age", Operator.GTE, 20)
                ))
            ))
        ));
        var shallow = new RuleValidator(catalog, 2);
        assertThatThrownBy(() -> shallow.validate(rule(deep)))
            .isInstanceOf(RuleValidationException.class)
            .hasMessageContaining("max depth");
    }
}
