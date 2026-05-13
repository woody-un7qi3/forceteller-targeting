package co.un7qi3.targeting.spring;

import co.un7qi3.targeting.core.attribute.AttributeProvider;
import co.un7qi3.targeting.core.attribute.AttributeRegistry;
import co.un7qi3.targeting.core.evaluator.RuleEvaluator;
import co.un7qi3.targeting.engine.serde.RuleJsonMapper;
import co.un7qi3.targeting.host.Targeting;
import com.fasterxml.jackson.databind.Module;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * forceteller-targeting Spring Boot 자동 설정.
 *
 * <p>호스트 도메인 타입에 의존하지 않는 4개 빈을 자동 등록한다.
 * 도메인 타입을 갖는 {@code InputAssembler<S>}, {@code TargetingResolver<S>}는
 * 호스트가 직접 {@code @Bean}으로 선언해야 한다 ({@link Targeting#assembler}, {@link Targeting#resolver} 참고).
 */
@AutoConfiguration
public class TargetingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RuleEvaluator ruleEvaluator() {
        return Targeting.defaultEvaluator();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuleJsonMapper ruleJsonMapper() {
        return Targeting.jsonMapper();
    }

    @Bean
    @ConditionalOnMissingBean(name = "targetingJacksonModule")
    public Module targetingJacksonModule() {
        return Targeting.jacksonModule();
    }

    @Bean
    @ConditionalOnMissingBean
    public AttributeRegistry attributeRegistry(List<AttributeProvider<?>> providers) {
        return AttributeRegistry.of(providers);
    }
}
