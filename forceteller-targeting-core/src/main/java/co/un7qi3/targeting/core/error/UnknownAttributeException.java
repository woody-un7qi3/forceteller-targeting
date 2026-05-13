package co.un7qi3.targeting.core.error;

public class UnknownAttributeException extends RuntimeException {

    private final String attribute;

    public UnknownAttributeException(String attribute) {
        super("Unknown attribute: " + attribute);
        this.attribute = attribute;
    }

    public String attribute() {
        return attribute;
    }
}
