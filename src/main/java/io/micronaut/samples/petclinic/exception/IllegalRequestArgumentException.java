package io.micronaut.samples.petclinic.exception;

/**
 * Signals invalid request data that does not satisfy domain validation.
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
}
