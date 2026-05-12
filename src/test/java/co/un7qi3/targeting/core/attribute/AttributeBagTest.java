package co.un7qi3.targeting.core.attribute;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttributeBagTest {

    @Test
    void put_and_get() {
        var bag = new AttributeBag();
        bag.put("user.age", 27).put("user.gender", "F");

        assertThat(bag.has("user.age")).isTrue();
        assertThat(bag.get("user.age")).isEqualTo(27);
        assertThat(bag.size()).isEqualTo(2);
    }

    @Test
    void putAll() {
        var bag = new AttributeBag();
        bag.putAll(Map.of("user.age", 27, "user.gender", "F"));
        assertThat(bag.size()).isEqualTo(2);
    }

    @Test
    void put_overrides_previous_value() {
        var bag = new AttributeBag().put("k", 1).put("k", 2);
        assertThat(bag.get("k")).isEqualTo(2);
    }

    @Test
    void allows_null_value() {
        var bag = new AttributeBag().put("k", null);
        assertThat(bag.has("k")).isTrue();
        assertThat(bag.get("k")).isNull();
    }

    @Test
    void rejects_blank_or_null_key() {
        var bag = new AttributeBag();
        assertThatThrownBy(() -> bag.put(null, "v")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> bag.put("", "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bag.put("  ", "v")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void snapshot_is_immutable() {
        var bag = new AttributeBag().put("k", 1);
        Map<String, Object> snap = bag.snapshot();

        assertThatThrownBy(() -> snap.put("x", 2))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void snapshot_is_decoupled_from_subsequent_changes() {
        var bag = new AttributeBag().put("k", 1);
        Map<String, Object> snap = bag.snapshot();

        bag.put("k", 999).put("new", "added");

        assertThat(snap.get("k")).isEqualTo(1);
        assertThat(snap.containsKey("new")).isFalse();
    }

    @Test
    void snapshot_allows_null_values() {
        var bag = new AttributeBag().put("k1", "v").put("k2", null);
        Map<String, Object> snap = bag.snapshot();
        assertThat(snap).containsEntry("k1", "v");
        assertThat(snap).containsEntry("k2", null);
    }

    @Test
    void preserves_insertion_order() {
        var bag = new AttributeBag()
            .put("c", 3).put("a", 1).put("b", 2);
        assertThat(bag.keys()).containsExactly("c", "a", "b");
    }
}
