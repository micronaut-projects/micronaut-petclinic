package io.micronaut.samples.petclinic.service;

/**
 * Signals that the supplied ETag no longer matches the persisted veterinarian.
 */
final class StaleEtagException extends Exception {

    StaleEtagException() {
        super("The entity etag has changed since it was loaded.");
    }
}
