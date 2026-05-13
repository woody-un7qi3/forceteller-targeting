package co.un7qi3.targeting.engine.serde;

import co.un7qi3.targeting.core.rule.Rule;
import co.un7qi3.targeting.core.rule.RuleNode;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Rule ↔ JSON 변환 진입점. 자체 ObjectMapper를 생성/관리한다.
 * 호스트의 ObjectMapper와 격리되어 라이브러리 동작이 호스트 정책에 영향받지 않는다.
 */
public final class RuleJsonMapper {

    private final ObjectMapper objectMapper;

    public RuleJsonMapper() {
        this.objectMapper = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .addModule(new TargetingJacksonModule())
            .build();
    }

    public String toJson(Rule rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize Rule", e);
        }
    }

    public String toJson(RuleNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize RuleNode", e);
        }
    }

    public Rule fromJson(String json) {
        try {
            return objectMapper.readValue(json, Rule.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize Rule: " + e.getMessage(), e);
        }
    }

    public RuleNode nodeFromJson(String json) {
        try {
            return objectMapper.readValue(json, RuleNode.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize RuleNode: " + e.getMessage(), e);
        }
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }
}
