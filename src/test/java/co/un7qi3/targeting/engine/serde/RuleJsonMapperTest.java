package co.un7qi3.targeting.engine.serde;

import co.un7qi3.targeting.core.rule.Operator;
import co.un7qi3.targeting.core.rule.Rule;
import co.un7qi3.targeting.core.rule.RuleNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleJsonMapperTest {

    private final RuleJsonMapper mapper = new RuleJsonMapper();

    @Test
    void serialize_and_parse_simple_rule() {
        Rule original = new Rule(1L, 1,
            new RuleNode.And("n0", List.of(
                new RuleNode.Compare("c_gender", "user.gender", Operator.EQ, "M"),
                new RuleNode.Compare("c_age", "user.age", Operator.GTE, 20)
            )),
            Map.of("label", "남성 20세 이상"));

        String json = mapper.toJson(original);
        assertThat(json).contains("\"type\":\"and\"");
        assertThat(json).contains("\"type\":\"compare\"");
        assertThat(json).contains("\"operator\":\"GTE\"");

        Rule parsed = mapper.fromJson(json);

        assertThat(parsed.id()).isEqualTo(1L);
        assertThat(parsed.version()).isEqualTo(1);
        assertThat(parsed.root()).isInstanceOf(RuleNode.And.class);

        var and = (RuleNode.And) parsed.root();
        assertThat(and.nodes()).hasSize(2);
    }

    @Test
    void roundtrip_nested_tree() {
        Rule original = new Rule(2L, 2,
            new RuleNode.And("n0", List.of(
                new RuleNode.Or("n1", List.of(
                    new RuleNode.Compare("c1", "user.country", Operator.EQ, "KR"),
                    new RuleNode.Compare("c2", "user.country", Operator.EQ, "JP")
                )),
                new RuleNode.Not("n2",
                    new RuleNode.Compare("c3", "event.flag", Operator.EQ, true))
            )), null);

        String json = mapper.toJson(original);
        Rule back = mapper.fromJson(json);

        assertThat(back.root()).isInstanceOf(RuleNode.And.class);
        var root = (RuleNode.And) back.root();
        assertThat(root.nodes().get(0)).isInstanceOf(RuleNode.Or.class);
        assertThat(root.nodes().get(1)).isInstanceOf(RuleNode.Not.class);
    }

    @Test
    void parses_between_with_min_max() {
        String json = """
            {
              "id":1,"version":1,
              "root":{"type":"compare","id":"c1","attribute":"user.age",
                      "operator":"BETWEEN","value":{"min":20,"max":29}}
            }""";

        Rule rule = mapper.fromJson(json);
        var cmp = (RuleNode.Compare) rule.root();
        assertThat(cmp.operator()).isEqualTo(Operator.BETWEEN);
        assertThat(cmp.value()).isInstanceOf(Map.class);
    }

    @Test
    void rejects_unknown_node_type() {
        String json = """
            {"id":1,"version":1,
             "root":{"type":"xor","id":"n0","nodes":[]}}""";

        assertThatThrownBy(() -> mapper.fromJson(json))
            .hasMessageContaining("xor");
    }

    @Test
    void rejects_unknown_operator() {
        String json = """
            {"id":1,"version":1,
             "root":{"type":"compare","id":"c1","attribute":"x",
                     "operator":"FOOBAR","value":1}}""";

        assertThatThrownBy(() -> mapper.fromJson(json))
            .hasMessageContaining("FOOBAR");
    }
}
