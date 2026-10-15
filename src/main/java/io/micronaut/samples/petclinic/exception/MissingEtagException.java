package io.micronaut.samples.petclinic.exception;

/**
 * Signals a mutation that did not provide its required ETag.
 */
public class MissingEtagException extends Exception {
    /**
     * Creates a missing-ETag failure without a custom message.
     */
    public MissingEtagException() {
    }

    /**
     * Creates a missing-ETag failure with a custom message.
     *
     * @param message explanation of the missing precondition
     */
    public MissingEtagException(String message) {
        super(message);
    }

    /**
     * Creates a missing-ETag failure with its underlying cause.
     *
     * @param message explanation of the missing precondition
     * @param cause the underlying failure
     */
    public MissingEtagException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a missing-ETag failure wrapping another exception.
     *
     * @param cause the underlying failure
     */
    public MissingEtagException(Throwable cause) {
        super(cause);
    }
}