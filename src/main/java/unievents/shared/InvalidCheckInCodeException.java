package unievents.shared;

/**
 * Thrown during UC4 (SD-04) when a student provides an invalid event check-in code.
 */
public class InvalidCheckInCodeException extends DomainException {
    public InvalidCheckInCodeException(String message) {
        super(message);
    }
}
