package co.un7qi3.targeting.core.evaluator;

import co.un7qi3.targeting.core.rule.Operator;

/**
 * 평가 trace의 한 줄. Compare 노드 한 개의 평가 결과를 기록한다.
 *
 * @param nodeId   평가된 Compare 노드의 id
 * @param attribute 비교 대상 속성 키
 * @param op       사용된 연산자
 * @param lhs      후보에서 꺼낸 값 (null 가능)
 * @param rhs      룰의 우변값
 * @param result   이 비교 노드의 결과
 */
public record TraceEntry(
    String nodeId,
    String attribute,
    Operator op,
    Object lhs,
    Object rhs,
    boolean result
) {
}
