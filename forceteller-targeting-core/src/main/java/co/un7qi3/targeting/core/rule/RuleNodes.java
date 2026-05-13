package co.un7qi3.targeting.core.rule;

import java.util.HashSet;
import java.util.Set;

/**
 * 룰 트리 분석 유틸.
 */
public final class RuleNodes {

    private RuleNodes() {}

    /**
     * 룰 트리에서 사용된 attribute 키 집합을 반환한다.
     * Compare 노드의 attribute만 수집. And/Or/Not은 자식들로 재귀.
     *
     * <p>호스트는 이 집합을 받아 “필요한 Provider만 호출 + 필요한 키만 채움” 최적화에 사용.
     */
    public static Set<String> usedAttributes(RuleNode root) {
        Set<String> keys = new HashSet<>();
        collect(root, keys);
        return keys;
    }

    private static void collect(RuleNode node, Set<String> keys) {
        switch (node) {
            case RuleNode.And a       -> a.nodes().forEach(child -> collect(child, keys));
            case RuleNode.Or  o       -> o.nodes().forEach(child -> collect(child, keys));
            case RuleNode.Not n       -> collect(n.node(), keys);
            case RuleNode.Compare cmp -> keys.add(cmp.attribute());
        }
    }
}
