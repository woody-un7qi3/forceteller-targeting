package co.un7qi3.targeting.core.attribute;

import co.un7qi3.targeting.core.rule.Operator;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributeCatalogTest {

    static class FakeProvider implements AttributeProvider<Object> {
        private final String namespace;
        private final List<AttributeSpec> specs;

        FakeProvider(String namespace, List<AttributeSpec> specs) {
            this.namespace = namespace;
            this.specs = specs;
        }

        @Override public String namespace() { return namespace; }
        @Override public List<AttributeSpec> declare() { return specs; }
        @Override public void fill(Object subject, AttributeBag bag, java.util.Set<String> neededKeys) { }
    }

    private static AttributeSpec spec(String key, AttributeType type, Operator... ops) {
        return new AttributeSpec(key, type, EnumSet.copyOf(List.of(ops)), null, null, null);
    }

    @Test
    void builds_catalog_from_providers() {
        var p1 = new FakeProvider("user", List.of(
            spec("user.age", AttributeType.INTEGER, Operator.GTE, Operator.LTE),
            spec("user.gender", AttributeType.STRING, Operator.EQ, Operator.IN)
        ));
        var p2 = new FakeProvider("force", List.of(
            spec("force.total", AttributeType.INTEGER, Operator.GTE)
        ));

        var catalog = AttributeCatalog.of(List.of(p1, p2));

        assertThat(catalog.size()).isEqualTo(3);
        assertThat(catalog.find("user.age")).isPresent();
        assertThat(catalog.find("force.total")).isPresent();
        assertThat(catalog.find("absent")).isEmpty();
    }

    @Test
    void require_throws_for_unknown_key() {
        var catalog = AttributeCatalog.of(List.of());
        assertThatThrownBy(() -> catalog.require("nope"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown attribute");
    }

    @Test
    void rejects_namespace_violation() {
        var bad = new FakeProvider("user", List.of(
            spec("device.platform", AttributeType.STRING, Operator.EQ)   // user.* 가 아님
        ));
        assertThatThrownBy(() -> AttributeCatalog.of(List.of(bad)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("outside its namespace");
    }

    @Test
    void rejects_duplicate_keys() {
        var p1 = new FakeProvider("user", List.of(spec("user.age", AttributeType.INTEGER, Operator.GTE)));
        var p2 = new FakeProvider("user", List.of(spec("user.age", AttributeType.INTEGER, Operator.EQ)));

        assertThatThrownBy(() -> AttributeCatalog.of(List.of(p1, p2)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate");
    }

    @Test
    void rejects_blank_namespace() {
        var p = new FakeProvider("", List.of());
        assertThatThrownBy(() -> AttributeCatalog.of(List.of(p)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void all_returns_unmodifiable() {
        var catalog = AttributeCatalog.of(List.of());
        assertThatThrownBy(() -> catalog.all().add(null))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
