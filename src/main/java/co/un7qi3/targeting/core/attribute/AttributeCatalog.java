package co.un7qi3.targeting.core.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 모든 AttributeProvider가 선언한 spec을 한 곳에 모은 메모리 카탈로그.
 *
 * <p>호스트 부팅 시 Provider들을 넘겨 빌드하고, 이후 검증/노출에 사용한다.
 */
public final class AttributeCatalog {

    private final Map<String, AttributeSpec> bySpec;

    private AttributeCatalog(Map<String, AttributeSpec> bySpec) {
        this.bySpec = Collections.unmodifiableMap(bySpec);
    }

    /**
     * Provider 목록으로부터 카탈로그를 빌드한다.
     *
     * <p>검증:
     * <ul>
     *   <li>모든 spec의 key는 Provider의 namespace prefix로 시작해야 한다 (예: namespace="user" → "user.*")</li>
     *   <li>전체 카탈로그 내 key 중복은 허용되지 않는다</li>
     * </ul>
     */
    public static AttributeCatalog of(List<? extends AttributeProvider<?>> providers) {
        var bySpec = new LinkedHashMap<String, AttributeSpec>();
        for (AttributeProvider<?> p : providers) {
            String ns = p.namespace();
            if (ns == null || ns.isBlank())
                throw new IllegalArgumentException("AttributeProvider.namespace must not be blank");
            String prefix = ns + ".";

            for (AttributeSpec spec : p.declare()) {
                if (!spec.key().startsWith(prefix)) {
                    throw new IllegalStateException(
                        "Provider " + p.getClass().getSimpleName() + " (ns=" + ns +
                        ") declared key '" + spec.key() + "' outside its namespace");
                }
                AttributeSpec previous = bySpec.put(spec.key(), spec);
                if (previous != null) {
                    throw new IllegalStateException(
                        "Duplicate attribute key: " + spec.key());
                }
            }
        }
        return new AttributeCatalog(bySpec);
    }

    public Optional<AttributeSpec> find(String key) {
        return Optional.ofNullable(bySpec.get(key));
    }

    public AttributeSpec require(String key) {
        AttributeSpec s = bySpec.get(key);
        if (s == null) throw new IllegalArgumentException("Unknown attribute: " + key);
        return s;
    }

    public Collection<AttributeSpec> all() {
        return bySpec.values();
    }

    public int size() {
        return bySpec.size();
    }
}
