package co.un7qi3.targeting.engine.validate;

import java.util.List;

public record ValidationReport(
    List<String> errors,
    List<String> warnings
) {
    public ValidationReport {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean ok() {
        return errors.isEmpty();
    }
}
