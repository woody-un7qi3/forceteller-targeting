package co.un7qi3.targeting.core.rule;

import java.util.List;

public sealed interface RuleNode
    permits RuleNode.And, RuleNode.Or, RuleNode.Not, RuleNode.Compare {

    String id();

    record And(String id, List<RuleNode> nodes) implements RuleNode {
        public And {
            if (id == null || id.isBlank())
                throw new IllegalArgumentException("And.id must not be blank");
            nodes = nodes == null ? List.of() : List.copyOf(nodes);
        }
    }

    record Or(String id, List<RuleNode> nodes) implements RuleNode {
        public Or {
            if (id == null || id.isBlank())
                throw new IllegalArgumentException("Or.id must not be blank");
            nodes = nodes == null ? List.of() : List.copyOf(nodes);
        }
    }

    record Not(String id, RuleNode node) implements RuleNode {
        public Not {
            if (id == null || id.isBlank())
                throw new IllegalArgumentException("Not.id must not be blank");
            if (node == null)
                throw new IllegalArgumentException("Not.node must not be null");
        }
    }

    record Compare(String id,
                   String attribute,
                   Operator operator,
                   Object value)
        implements RuleNode {
        public Compare {
            if (id == null || id.isBlank())
                throw new IllegalArgumentException("Compare.id must not be blank");
            if (attribute == null || attribute.isBlank())
                throw new IllegalArgumentException("Compare.attribute must not be blank");
            if (operator == null)
                throw new IllegalArgumentException("Compare.operator must not be null");
        }
    }
}
