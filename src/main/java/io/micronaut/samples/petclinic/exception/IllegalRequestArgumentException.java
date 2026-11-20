package io.micronaut.samples.petclinic.exception;

/**
 * Signals request data or state that the application cannot accept.
 */
public class IllegalRequestArgumentException extends Exception {

    /**
     * Creates a request failure with a message suitable for the client.
     *
     * @param message description of the invalid request
     */
    public IllegalRequestArgumentException(String message) {
        super(message);
    }

    /**
     * Creates a request failure with its underlying cause.
     *
     * @param message description of the invalid request
     * @param cause underlying failure
     */
    public IllegalRequestArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a request failure caused by another exception.
     *
     * @param cause underlying failure
     */
    public IllegalRequestArgumentException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a request failure with custom suppression and stack-trace behavior.
     *
     * @param message description of the invalid request
     * @param cause underlying failure
     * @param enableSuppression whether suppressed exceptions are recorded
     * @param writableStackTrace whether the stack trace can be written
     */
    public IllegalRequestArgumentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /** Creates a request failure without a message or cause. */
    public IllegalRequestArgumentException() {
    }
}
