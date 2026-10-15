package io.micronaut.samples.petclinic.exception;

/**
 * Signals an invalid domain request, such as an unknown speciality.
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
