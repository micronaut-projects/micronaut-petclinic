package io.micronaut.samples.petclinic.exception;

/**
 * Signals that a supplied precondition does not match the current resource state.
 */
public final class PreconditionFailedException extends Exception {

    public PreconditionFailedException() {
        super("The entity etag has changed since it was loaded.");
    }

    /**
     * Creates a failed-precondition failure.
     *
     * @param message explanation of the failed precondition
     */
    public PreconditionFailedException(String message) {
        super(message);
    }

    /**
     * Creates a failed-precondition failure with its service-level cause.
     *
     * @param message explanation of the failed precondition
     * @param cause the underlying condition
     */
    public PreconditionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
