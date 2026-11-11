package io.micronaut.samples.petclinic.exception;

/** Signals file data that does not match an accepted format or its validation constraints. */
public class InvalidFileFormatException extends IllegalRequestArgumentException {
    /**
     * Creates an invalid-format failure with a client-safe message.
     */
    public InvalidFileFormatException() {
        super("The uploaded file format is invalid.");
    }
}
