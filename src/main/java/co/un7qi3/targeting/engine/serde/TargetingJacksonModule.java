package co.un7qi3.targeting.engine.serde;

import co.un7qi3.targeting.core.rule.RuleNode;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson 모듈. RuleNode sealed 계층의 type 디스크리미네이터를 등록한다.
 */
public final class TargetingJacksonModule extends SimpleModule {

    public TargetingJacksonModule() {
        setMixInAnnotation(RuleNode.class, RuleNodeMixin.class);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = RuleNode.And.class,     name = "and"),
        @JsonSubTypes.Type(value = RuleNode.Or.class,      name = "or"),
        @JsonSubTypes.Type(value = RuleNode.Not.class,     name = "not"),
        @JsonSubTypes.Type(value = RuleNode.Compare.class, name = "compare")
    })
    private abstract static class RuleNodeMixin { }
}
