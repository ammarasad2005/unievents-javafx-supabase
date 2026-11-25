package unievents.shared;

/**
 * Wraps JDBC / persistence failures so SQLException never leaks uncaught to UI.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
