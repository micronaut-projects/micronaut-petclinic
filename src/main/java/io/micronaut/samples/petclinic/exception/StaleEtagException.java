package io.micronaut.samples.petclinic.exception;

/**
 * Signals that the supplied ETag no longer matches the persisted veterinarian.
 */
public final class StaleEtagException extends Exception {
    /**
     * Creates a stale-ETag failure with the standard conflict message.
     */
    public StaleEtagException() {
        super("The entity etag has changed since it was loaded.");
    }
}