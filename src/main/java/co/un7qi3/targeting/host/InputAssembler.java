package co.un7qi3.targeting.host;

import co.un7qi3.targeting.core.attribute.AttributeBag;
import co.un7qi3.targeting.core.attribute.AttributeProvider;
import co.un7qi3.targeting.core.evaluation.EvaluationInput;
import co.un7qi3.targeting.core.evaluation.MapEvaluationInput;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * 호스트 도메인 객체를 {@link EvaluationInput}으로 조립한다.
 *
 * <p>2단계 스킵으로 비용 최소화:
 * <ol>
 *   <li>namespace 단위 — {@code neededKeys}가 가리키는 namespace의 Provider만 호출</li>
 *   <li>키 단위 — Provider 내부에서 {@code if (neededKeys.contains(...))} 분기로 일부 키만 채움</li>
 * </ol>
 *
 * @param <S> 호스트의 평가 대상 타입 (예: User, Account)
 */
public final class InputAssembler<S> {

    private final List<AttributeProvider<S>> providers;
    private final Function<S, String> idExtractor;

    /**
     * @param providers   호스트가 구현한 Provider 목록
     * @param idExtractor target → 식별자 문자열 추출 함수 (로깅/추적용)
     */
    public InputAssembler(List<AttributeProvider<S>> providers, Function<S, String> idExtractor) {
        Objects.requireNonNull(providers, "providers");
        Objects.requireNonNull(idExtractor, "idExtractor");
        this.providers = List.copyOf(providers);
        this.idExtractor = idExtractor;
    }

    /**
     * 룰이 실제로 참조하는 키({@code neededKeys})만 채워 EvaluationInput을 만든다.
     */
    public EvaluationInput assembleFor(S target, Set<String> neededKeys) {
        AttributeBag bag = new AttributeBag();

        if (!neededKeys.isEmpty()) {
            Set<String> neededNamespaces = extractNamespaces(neededKeys);
            for (AttributeProvider<S> p : providers) {
                if (!neededNamespaces.contains(p.namespace())) continue;
                p.fill(target, bag, neededKeys);
            }
        }

        String id = target == null ? "anonymous" : idExtractor.apply(target);
        return new MapEvaluationInput(id, bag.snapshot(), Instant.now());
    }

    private static Set<String> extractNamespaces(Set<String> keys) {
        Set<String> ns = new HashSet<>();
        for (String k : keys) {
            int dot = k.indexOf('.');
            ns.add(dot < 0 ? k : k.substring(0, dot));
        }
        return ns;
    }
}
