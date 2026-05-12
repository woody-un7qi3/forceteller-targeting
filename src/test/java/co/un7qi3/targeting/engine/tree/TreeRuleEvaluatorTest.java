package co.un7qi3.targeting.engine.tree;

import co.un7qi3.targeting.core.evaluation.EvaluationInput;
import co.un7qi3.targeting.core.evaluation.MapEvaluationInput;
import co.un7qi3.targeting.core.evaluator.EvaluationResult;
import co.un7qi3.targeting.core.evaluator.RuleEvaluator;
import co.un7qi3.targeting.core.rule.Operator;
import co.un7qi3.targeting.core.rule.Rule;
import co.un7qi3.targeting.core.rule.RuleNode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TreeRuleEvaluatorTest {

    private final RuleEvaluator evaluator = new TreeRuleEvaluator();

    private EvaluationInput candidate(Map<String, Object> attrs) {
        return new MapEvaluationInput("u1", attrs, Instant.now());
    }

    private Rule rule(RuleNode root) {
        return new Rule(1L, 1, root, null);
    }

    @Test
    void single_compare_matches() {
        var rule = rule(new RuleNode.Compare("c1", "user.age", Operator.GTE, 20));
        var c = candidate(Map.of("user.age", 27));

        EvaluationResult r = evaluator.eval(rule, c);

        assertThat(r.matched()).isTrue();
        assertThat(r.trace()).hasSize(1);
        assertThat(r.trace().get(0).nodeId()).isEqualTo("c1");
        assertThat(r.trace().get(0).result()).isTrue();
    }

    @Test
    void single_compare_does_not_match() {
        var rule = rule(new RuleNode.Compare("c1", "user.age", Operator.GTE, 20));
        var c = candidate(Map.of("user.age", 15));

        assertThat(evaluator.eval(rule, c).matched()).isFalse();
    }

    @Test
    void and_requires_all_true() {
        var rule = rule(new RuleNode.And("n0", List.of(
            new RuleNode.Compare("c1", "user.gender", Operator.EQ, "M"),
            new RuleNode.Compare("c2", "user.age", Operator.GTE, 20)
        )));

        assertThat(evaluator.eval(rule, candidate(Map.of("user.gender", "M", "user.age", 27))).matched()).isTrue();
        assertThat(evaluator.eval(rule, candidate(Map.of("user.gender", "F", "user.age", 27))).matched()).isFalse();
        assertThat(evaluator.eval(rule, candidate(Map.of("user.gender", "M", "user.age", 18))).matched()).isFalse();
    }

    @Test
    void or_requires_one_true() {
        var rule = rule(new RuleNode.Or("n0", List.of(
            new RuleNode.Compare("c1", "user.country", Operator.EQ, "KR"),
            new RuleNode.Compare("c2", "user.country", Operator.EQ, "JP")
        )));

        assertThat(evaluator.eval(rule, candidate(Map.of("user.country", "KR"))).matched()).isTrue();
        assertThat(evaluator.eval(rule, candidate(Map.of("user.country", "JP"))).matched()).isTrue();
        assertThat(evaluator.eval(rule, candidate(Map.of("user.country", "US"))).matched()).isFalse();
    }

    @Test
    void not_inverts_result() {
        var rule = rule(new RuleNode.Not("n0",
            new RuleNode.Compare("c1", "event.flag", Operator.EQ, true)));

        assertThat(evaluator.eval(rule, candidate(Map.of("event.flag", true))).matched()).isFalse();
        assertThat(evaluator.eval(rule, candidate(Map.of("event.flag", false))).matched()).isTrue();
    }

    @Test
    void empty_and_is_true() {
        var rule = rule(new RuleNode.And("n0", List.of()));
        assertThat(evaluator.eval(rule, candidate(Map.of())).matched()).isTrue();
    }

    @Test
    void empty_or_is_false() {
        var rule = rule(new RuleNode.Or("n0", List.of()));
        assertThat(evaluator.eval(rule, candidate(Map.of())).matched()).isFalse();
    }

    @Test
    void missing_attribute_is_safe_false() {
        var rule = rule(new RuleNode.Compare("c1", "user.age", Operator.GTE, 20));
        assertThat(evaluator.eval(rule, candidate(Map.of())).matched()).isFalse();
    }

    @Test
    void nested_tree() {
        // (KR + 20대) OR (JP + 30대), AND NOT lucky luck
        var rule = rule(new RuleNode.And("n0", List.of(
            new RuleNode.Or("n1", List.of(
                new RuleNode.And("n11", List.of(
                    new RuleNode.Compare("c1", "user.country", Operator.EQ, "KR"),
                    new RuleNode.Compare("c2", "user.age", Operator.BETWEEN,
                        Map.of("min", 20, "max", 29))
                )),
                new RuleNode.And("n12", List.of(
                    new RuleNode.Compare("c3", "user.country", Operator.EQ, "JP"),
                    new RuleNode.Compare("c4", "user.age", Operator.BETWEEN,
                        Map.of("min", 30, "max", 39))
                ))
            )),
            new RuleNode.Not("n2",
                new RuleNode.Compare("c5", "event.isLuckyLuckUser", Operator.EQ, true))
        )));

        // KR 25세, lucky luck 아님 → 매칭
        assertThat(evaluator.eval(rule, candidate(Map.of(
            "user.country", "KR",
            "user.age", 25,
            "event.isLuckyLuckUser", false
        ))).matched()).isTrue();

        // JP 35세, lucky luck 아님 → 매칭
        assertThat(evaluator.eval(rule, candidate(Map.of(
            "user.country", "JP",
            "user.age", 35,
            "event.isLuckyLuckUser", false
        ))).matched()).isTrue();

        // KR 25세이지만 lucky luck → 비매칭
        assertThat(evaluator.eval(rule, candidate(Map.of(
            "user.country", "KR",
            "user.age", 25,
            "event.isLuckyLuckUser", true
        ))).matched()).isFalse();

        // US 25세 → 비매칭
        assertThat(evaluator.eval(rule, candidate(Map.of(
            "user.country", "US",
            "user.age", 25,
            "event.isLuckyLuckUser", false
        ))).matched()).isFalse();
    }

    @Test
    void trace_records_each_compare() {
        var rule = rule(new RuleNode.And("n0", List.of(
            new RuleNode.Compare("c1", "user.gender", Operator.EQ, "M"),
            new RuleNode.Compare("c2", "user.age", Operator.GTE, 20)
        )));

        EvaluationResult r = evaluator.eval(rule, candidate(Map.of("user.gender", "M", "user.age", 27)));

        assertThat(r.trace()).hasSize(2);
        assertThat(r.trace()).extracting("nodeId").containsExactly("c1", "c2");
    }

    @Test
    void and_short_circuits_on_first_false() {
        var rule = rule(new RuleNode.And("n0", List.of(
            new RuleNode.Compare("c1", "user.age", Operator.GTE, 100),  // false
            new RuleNode.Compare("c2", "user.gender", Operator.EQ, "M")  // 평가 안 됨
        )));

        EvaluationResult r = evaluator.eval(rule, candidate(Map.of("user.age", 27, "user.gender", "M")));

        assertThat(r.matched()).isFalse();
        assertThat(r.trace()).hasSize(1);  // c2는 trace에 안 들어옴
        assertThat(r.trace().get(0).nodeId()).isEqualTo("c1");
    }
}
