package co.un7qi3.targeting.core.attribute;

import co.un7qi3.targeting.core.rule.Operator;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributeRegistryTest {

    static class FakeProvider implements AttributeProvider<Object> {
        private final String namespace;
        private final List<AttributeSpec> specs;

        FakeProvider(String namespace, List<AttributeSpec> specs) {
            this.namespace = namespace;
            this.specs = specs;
        }

        @Override public String namespace() { return namespace; }
        @Override public List<AttributeSpec> declare() { return specs; }
        @Override public void fill(Object target, AttributeBag bag, java.util.Set<String> neededKeys) { }
    }

    private static AttributeSpec spec(String key, AttributeType type, Operator... ops) {
        return new AttributeSpec(key, type, EnumSet.copyOf(List.of(ops)), null, null, null);
    }

    @Test
    void builds_registry_from_providers() {
        var p1 = new FakeProvider("user", List.of(
            spec("user.age", AttributeType.INTEGER, Operator.GTE, Operator.LTE),
            spec("user.gender", AttributeType.STRING, Operator.EQ, Operator.IN)
        ));
        var p2 = new FakeProvider("force", List.of(
            spec("force.total", AttributeType.INTEGER, Operator.GTE)
        ));

        var registry = AttributeRegistry.of(List.of(p1, p2));

        assertThat(registry.size()).isEqualTo(3);
        assertThat(registry.find("user.age")).isPresent();
        assertThat(registry.find("force.total")).isPresent();
        assertThat(registry.find("absent")).isEmpty();
    }

    @Test
    void require_throws_for_unknown_key() {
        var registry = AttributeRegistry.of(List.of());
        assertThatThrownBy(() -> registry.require("nope"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown attribute");
    }

    @Test
    void rejects_namespace_violation() {
        var bad = new FakeProvider("user", List.of(
            spec("device.platform", AttributeType.STRING, Operator.EQ)   // user.* 가 아님
        ));
        assertThatThrownBy(() -> AttributeRegistry.of(List.of(bad)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("outside its namespace");
    }

    @Test
    void rejects_duplicate_keys() {
        var p1 = new FakeProvider("user", List.of(spec("user.age", AttributeType.INTEGER, Operator.GTE)));
        var p2 = new FakeProvider("user", List.of(spec("user.age", AttributeType.INTEGER, Operator.EQ)));

        assertThatThrownBy(() -> AttributeRegistry.of(List.of(p1, p2)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate");
    }

    @Test
    void rejects_blank_namespace() {
        var p = new FakeProvider("", List.of());
        assertThatThrownBy(() -> AttributeRegistry.of(List.of(p)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void all_returns_unmodifiable() {
        var registry = AttributeRegistry.of(List.of());
        assertThatThrownBy(() -> registry.all().add(null))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
