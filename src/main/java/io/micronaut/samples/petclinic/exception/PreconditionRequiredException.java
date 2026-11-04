package io.micronaut.samples.petclinic.exception;

/**
 * Signals that a request is missing a precondition required to perform the operation.
 */
public final class PreconditionRequiredException extends Exception {


    public PreconditionRequiredException() {
        super("ETag is missing.");
    }

    /**
     * Creates a missing-precondition failure.
     *
     * @param message explanation of the missing precondition
     */
    public PreconditionRequiredException(String message) {
        super(message);
    }

    /**
     * Creates a missing-precondition failure with its service-level cause.
     *
     * @param message explanation of the missing precondition
     * @param cause the underlying condition
     */
    public PreconditionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
