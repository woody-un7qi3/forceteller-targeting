package co.un7qi3.targeting.engine.tree;

import co.un7qi3.targeting.core.evaluation.EvaluationInput;
import co.un7qi3.targeting.core.evaluator.EvaluationResult;
import co.un7qi3.targeting.core.evaluator.RuleEvaluator;
import co.un7qi3.targeting.core.evaluator.TraceEntry;
import co.un7qi3.targeting.core.rule.Rule;
import co.un7qi3.targeting.core.rule.RuleNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 룰 트리를 재귀로 평가하는 기본 구현체.
 * AND/OR은 short-circuit, NOT은 부정, Compare는 lhs/rhs 비교 후 trace 기록.
 */
public final class TreeRuleEvaluator implements RuleEvaluator {

    @Override
    public EvaluationResult eval(Rule rule, EvaluationInput input) {
        List<TraceEntry> trace = new ArrayList<>();
        boolean matched = evalNode(rule.root(), input, trace);
        return new EvaluationResult(matched, trace, Instant.now());
    }

    private boolean evalNode(RuleNode node, EvaluationInput c, List<TraceEntry> trace) {
        return switch (node) {
            case RuleNode.And a -> {
                for (RuleNode child : a.nodes()) {
                    if (!evalNode(child, c, trace)) { yield false; }
                }
                yield true;
            }
            case RuleNode.Or o -> {
                if (o.nodes().isEmpty()) { yield false; }
                for (RuleNode child : o.nodes()) {
                    if (evalNode(child, c, trace)) { yield true; }
                }
                yield false;
            }
            case RuleNode.Not n -> !evalNode(n.node(), c, trace);
            case RuleNode.Compare cmp -> {
                Object lhs = c.attr(cmp.attribute()).orElse(null);
                boolean r;
                try {
                    r = cmp.operator().apply(lhs, cmp.value());
                } catch (RuntimeException e) {
                    r = false;   // 비교 실패는 안전한 디폴트
                }
                trace.add(new TraceEntry(cmp.id(), cmp.attribute(), cmp.operator(), lhs, cmp.value(), r));
                yield r;
            }
        };
    }
}
