package co.un7qi3.targeting.core.evaluator;

import co.un7qi3.targeting.core.evaluation.EvaluationInput;
import co.un7qi3.targeting.core.rule.Rule;

/**
 * 룰 평가기 표준 인터페이스.
 * 기본 구현체는 {@code engine/tree/TreeRuleEvaluator}.
 */
public interface RuleEvaluator {
    EvaluationResult eval(Rule rule, EvaluationInput input);
}
