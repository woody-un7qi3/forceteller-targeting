package co.un7qi3.targeting.engine.validate;

import co.un7qi3.targeting.core.attribute.AttributeRegistry;
import co.un7qi3.targeting.core.attribute.AttributeSpec;
import co.un7qi3.targeting.core.attribute.AttributeStatus;
import co.un7qi3.targeting.core.error.RuleValidationException;
import co.un7qi3.targeting.core.rule.Rule;
import co.un7qi3.targeting.core.rule.RuleNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 룰 저장/활성화 전 검증.
 * <ul>
 *   <li>Compare 노드의 attribute가 레지스트리에 있는지</li>
 *   <li>Compare 노드의 op가 spec.allowedOps에 속하는지</li>
 *   <li>노드 id가 트리 내에서 유일한지</li>
 *   <li>트리 깊이가 제한을 넘지 않는지</li>
 *   <li>DEPRECATED 속성 사용은 경고로</li>
 * </ul>
 */
public final class RuleValidator {

    public static final int DEFAULT_MAX_DEPTH = 10;

    private final AttributeRegistry registry;
    private final int maxDepth;

    public RuleValidator(AttributeRegistry registry) {
        this(registry, DEFAULT_MAX_DEPTH);
    }

    public RuleValidator(AttributeRegistry registry, int maxDepth) {
        this.registry = registry;
        this.maxDepth = maxDepth;
    }

    /** 검증 후 에러가 있으면 예외, 없으면 정상 반환. 경고는 report로만 노출. */
    public ValidationReport validate(Rule rule) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        walk(rule.root(), 0, errors, warnings, seenIds);

        ValidationReport report = new ValidationReport(errors, warnings);
        if (!report.ok()) throw new RuleValidationException(errors);
        return report;
    }

    private void walk(RuleNode node, int depth,
                      List<String> errors, List<String> warnings, Set<String> seenIds) {
        if (depth > maxDepth) {
            errors.add("max depth exceeded at node " + node.id() + " (depth=" + depth + ")");
            return;
        }
        if (!seenIds.add(node.id())) {
            errors.add("duplicate node id: " + node.id());
        }

        switch (node) {
            case RuleNode.And a -> {
                for (RuleNode child : a.nodes()) walk(child, depth + 1, errors, warnings, seenIds);
            }
            case RuleNode.Or o -> {
                for (RuleNode child : o.nodes()) walk(child, depth + 1, errors, warnings, seenIds);
            }
            case RuleNode.Not n -> walk(n.node(), depth + 1, errors, warnings, seenIds);
            case RuleNode.Compare cmp -> validateCompare(cmp, errors, warnings);
        }
    }

    private void validateCompare(RuleNode.Compare cmp, List<String> errors, List<String> warnings) {
        var spec = registry.find(cmp.attribute()).orElse(null);
        if (spec == null) {
            errors.add("unknown attribute: " + cmp.attribute() + " (node " + cmp.id() + ")");
            return;
        }
        if (!spec.allows(cmp.operator())) {
            errors.add("operator " + cmp.operator() + " not allowed for attribute " + cmp.attribute()
                + " (node " + cmp.id() + ")");
        }
        if (spec.status() == AttributeStatus.DEPRECATED) {
            warnings.add("attribute " + cmp.attribute() + " is DEPRECATED (node " + cmp.id() + ")");
        }
        // value 타입 정합성은 첫 버전에선 검사 생략 (스키마/평가기에서 잡힘)
        _unused(spec);
    }

    @SuppressWarnings("unused")
    private static void _unused(AttributeSpec spec) { }
}
