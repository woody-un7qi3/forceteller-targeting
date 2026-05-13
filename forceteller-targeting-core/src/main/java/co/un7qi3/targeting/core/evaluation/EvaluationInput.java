package co.un7qi3.targeting.core.evaluation;

import java.time.Instant;
import java.util.Optional;

/**
 * 룰 평가의 입력. 한 평가 대상의 속성들을 담은 불변 가방.
 * {@link co.un7qi3.targeting.core.evaluator.RuleEvaluator}가 소비하는 데이터.
 */
public interface EvaluationInput {

    /** 평가 대상 식별자 (보통 userId, 익명이면 디바이스 ID). 로깅/trace용. */
    String id();

    /** 평가 시점. 시뮬레이션/재현용. */
    Instant evaluatedAt();

    /**
     * 속성 키로 값을 꺼낸다.
     *
     * @param key 카탈로그에 등록된 속성 키 (예: "user.gender")
     * @return 값 또는 빈 Optional (속성이 없거나 null이면 empty)
     */
    Optional<Object> attr(String key);
}
