package co.un7qi3.targeting.host;

import co.un7qi3.targeting.core.evaluation.EvaluationInput;
import co.un7qi3.targeting.core.evaluator.EvaluationResult;
import co.un7qi3.targeting.core.evaluator.RuleEvaluator;
import co.un7qi3.targeting.core.rule.Rule;
import co.un7qi3.targeting.core.rule.RuleNodes;

import java.util.Objects;
import java.util.Set;

/**
 * 호스트가 사용하는 타게팅 평가 진입점.
 * 입력 조립({@link InputAssembler}) + 룰 평가({@link RuleEvaluator})를 한 메서드로 묶는다.
 *
 * @param <S> 호스트의 평가 대상 타입
 */
public final class TargetingResolver<S> {

    private final InputAssembler<S> assembler;
    private final RuleEvaluator evaluator;

    public TargetingResolver(InputAssembler<S> assembler, RuleEvaluator evaluator) {
        this.assembler = Objects.requireNonNull(assembler, "assembler");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    /**
     * 한 target에 대해 한 룰을 평가한다.
     */
    public EvaluationResult evaluate(Rule rule, S target) {
        Set<String> neededKeys = RuleNodes.usedAttributes(rule.root());
        EvaluationInput input = assembler.assembleFor(target, neededKeys);
        return evaluator.eval(rule, input);
    }
}
