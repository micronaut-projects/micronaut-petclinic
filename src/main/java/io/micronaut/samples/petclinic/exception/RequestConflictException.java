package io.micronaut.samples.petclinic.exception;

/** Signals a request that conflicts with a concurrent change. */
public class RequestConflictException extends IllegalRequestArgumentException {
    /**
     * Creates a conflict failure with a client-safe message.
     *
     * @param message description of the conflict suitable for the client
     */
    public RequestConflictException(String message) {
        super(message);
    }
}
