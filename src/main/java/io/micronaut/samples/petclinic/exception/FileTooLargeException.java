package io.micronaut.samples.petclinic.exception;

/** Signals file data that exceeds the configured byte limit. */
public class FileTooLargeException extends IllegalRequestArgumentException {
    /**
     * Creates a file-size failure with a client-safe message.
     */
    public FileTooLargeException() {
        super("The uploaded file exceeds the maximum size.");
    }
}
