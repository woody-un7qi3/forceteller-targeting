package co.un7qi3.targeting.core.evaluator;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 룰 평가의 결과.
 *
 * @param matched     최종 매칭 여부
 * @param trace       각 Compare 노드의 결과 (디버깅·감사용)
 * @param evaluatedAt 평가 시각
 */
public record EvaluationResult(
    boolean matched,
    List<TraceEntry> trace,
    Instant evaluatedAt
) {
    public EvaluationResult {
        trace = trace == null ? List.of() : List.copyOf(trace);
        evaluatedAt = Objects.requireNonNullElseGet(evaluatedAt, Instant::now);
    }
}
