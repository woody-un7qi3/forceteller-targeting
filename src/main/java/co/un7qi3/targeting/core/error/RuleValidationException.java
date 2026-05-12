package co.un7qi3.targeting.core.error;

import java.util.List;

public class RuleValidationException extends RuntimeException {

    private final List<String> errors;

    public RuleValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public RuleValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
