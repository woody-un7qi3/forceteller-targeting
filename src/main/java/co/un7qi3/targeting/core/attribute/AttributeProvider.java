package co.un7qi3.targeting.core.attribute;

import java.util.List;
import java.util.Set;

/**
 * 호스트가 구현하는 속성 공급자.
 *
 * <p>한 Provider는 한 네임스페이스 영역을 책임진다.
 * 예: {@code UserAttributeProvider}는 {@code user.*} 키를 declare하고 채운다.
 *
 * @param <S> 속성 채우기에 필요한 호스트 도메인 객체 타입 (예: User)
 */
public interface AttributeProvider<S> {

    /**
     * 자기 영역 prefix. 예: "user", "force", "device".
     * declare()가 반환하는 모든 spec의 key는 이 prefix로 시작해야 한다.
     */
    String namespace();

    /**
     * 자기가 다루는 속성 spec 목록.
     * AttributeRegistry가 시작 시 호출해 레지스트리를 구성한다.
     */
    List<AttributeSpec> declare();

    /**
     * 후보의 속성을 가방에 채운다. neededKeys에 포함된 키만 채우면 된다.
     *
     * <p>구현 가이드:
     * <ul>
     *   <li>각 키마다 {@code if (neededKeys.contains("...")) bag.put(...)} 분기</li>
     *   <li>같은 데이터 출처(예: 1회 DB 조회)에서 여러 키가 나오면 그룹으로 묶어 호출 최소화</li>
     *   <li>모든 키가 불필요하면 즉시 return</li>
     * </ul>
     *
     * @param subject     후보 도메인 객체 (보통 User)
     * @param bag         값을 넣을 가방
     * @param neededKeys  룰이 실제로 참조하는 키 집합 (이 키만 채우면 됨)
     */
    void fill(S subject, AttributeBag bag, Set<String> neededKeys);
}
