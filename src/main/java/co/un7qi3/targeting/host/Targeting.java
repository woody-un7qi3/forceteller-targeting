package co.un7qi3.targeting.host;

import co.un7qi3.targeting.core.attribute.AttributeRegistry;
import co.un7qi3.targeting.core.attribute.AttributeProvider;
import co.un7qi3.targeting.core.evaluator.RuleEvaluator;
import co.un7qi3.targeting.engine.serde.RuleJsonMapper;
import co.un7qi3.targeting.engine.serde.TargetingJacksonModule;
import co.un7qi3.targeting.engine.tree.TreeRuleEvaluator;
import com.fasterxml.jackson.databind.Module;

import java.util.List;
import java.util.function.Function;

/**
 * 라이브러리 사용 진입점 (Facade).
 *
 * <p>호스트가 등록해야 하는 모든 부품을 정적 메서드로 노출한다.
 * 새 호스트는 이 클래스의 자동완성만 봐도 "무엇이 필요한지"를 한눈에 알 수 있다.
 *
 * <p>전형적인 Spring 호스트 사용 예:
 * <pre>{@code
 * @Configuration
 * public class TargetingConfig {
 *     @Bean public RuleEvaluator evaluator()        { return Targeting.defaultEvaluator(); }
 *     @Bean public RuleJsonMapper jsonMapper()      { return Targeting.jsonMapper(); }
 *     @Bean public Module jacksonModule()           { return Targeting.jacksonModule(); }
 *
 *     @Bean public AttributeRegistry registry(List<AttributeProvider<User>> ps) {
 *         return Targeting.registry(ps);
 *     }
 *     @Bean public InputAssembler<User> assembler(List<AttributeProvider<User>> ps) {
 *         return Targeting.assembler(ps, u -> String.valueOf(u.getId()));
 *     }
 *     @Bean public TargetingResolver<User> resolver(InputAssembler<User> a, RuleEvaluator e) {
 *         return Targeting.resolver(a, e);
 *     }
 * }
 * }</pre>
 */
public final class Targeting {

    private Targeting() {}

    /**
     * 룰 평가기 기본 구현. 트리 순회 기반 평가.
     */
    public static RuleEvaluator defaultEvaluator() {
        return new TreeRuleEvaluator();
    }

    /**
     * Rule ↔ JSON 변환기. 자체 ObjectMapper를 사용한다 (호스트 mapper와 격리).
     */
    public static RuleJsonMapper jsonMapper() {
        return new RuleJsonMapper();
    }

    /**
     * Spring MVC 전역 ObjectMapper에 등록할 Jackson 모듈.
     * {@code RuleNode} sealed 타입 디스크리미네이터를 다룬다.
     */
    public static Module jacksonModule() {
        return new TargetingJacksonModule();
    }

    /**
     * Provider 목록으로 레지스트리를 만든다. 부팅 시 1회 호출.
     */
    public static <S> AttributeRegistry registry(List<AttributeProvider<S>> providers) {
        return AttributeRegistry.of(providers);
    }

    /**
     * 호스트 도메인 객체 → {@link co.un7qi3.targeting.core.evaluation.EvaluationInput} 조립.
     *
     * @param providers   호스트가 구현한 Provider 목록
     * @param idExtractor target에서 식별자 문자열을 뽑는 함수 (로깅/트레이스용)
     */
    public static <S> InputAssembler<S> assembler(
            List<AttributeProvider<S>> providers,
            Function<S, String> idExtractor) {
        return new InputAssembler<>(providers, idExtractor);
    }

    /**
     * 평가 진입점. {@link InputAssembler}와 {@link RuleEvaluator}를 묶는다.
     */
    public static <S> TargetingResolver<S> resolver(
            InputAssembler<S> assembler,
            RuleEvaluator evaluator) {
        return new TargetingResolver<>(assembler, evaluator);
    }
}
