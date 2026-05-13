package co.un7qi3.targeting.core.evaluation;

import co.un7qi3.targeting.core.attribute.AttributeBag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapEvaluationInputTest {

    @Test
    void exposes_id_and_evaluatedAt() {
        Instant t = Instant.parse("2026-05-11T05:00:00Z");
        var c = new MapEvaluationInput("user-12345", Map.of("user.age", 27), t);

        assertThat(c.id()).isEqualTo("user-12345");
        assertThat(c.evaluatedAt()).isEqualTo(t);
    }

    @Test
    void attr_returns_value_when_present() {
        var c = new MapEvaluationInput("u1", Map.of("user.age", 27), Instant.now());
        assertThat(c.attr("user.age")).contains(27);
    }

    @Test
    void attr_returns_empty_when_key_absent() {
        var c = new MapEvaluationInput("u1", Map.of("user.age", 27), Instant.now());
        assertThat(c.attr("user.gender")).isEmpty();
    }

    @Test
    void attr_returns_empty_for_null_key() {
        var c = new MapEvaluationInput("u1", Map.of("user.age", 27), Instant.now());
        assertThat(c.attr(null)).isEmpty();
    }

    @Test
    void attributes_are_decoupled_from_external_map() {
        Map<String, Object> mutable = new HashMap<>();
        mutable.put("user.age", 27);

        var c = new MapEvaluationInput("u1", mutable, Instant.now());

        mutable.put("user.age", 999);
        mutable.put("user.gender", "M");

        assertThat(c.attr("user.age")).contains(27);
        assertThat(c.attr("user.gender")).isEmpty();
    }

    @Test
    void rejects_null_constructor_args() {
        assertThatThrownBy(() -> new MapEvaluationInput(null, Map.of(), Instant.now()))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MapEvaluationInput("u1", null, Instant.now()))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new MapEvaluationInput("u1", Map.of(), null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void integrates_with_attribute_bag_snapshot() {
        var bag = new AttributeBag()
            .put("user.age", 27)
            .put("user.gender", "F");

        EvaluationInput c = new MapEvaluationInput("u1", bag.snapshot(), Instant.now());

        assertThat(c.attr("user.age")).contains(27);
        assertThat(c.attr("user.gender")).contains("F");
    }
}
