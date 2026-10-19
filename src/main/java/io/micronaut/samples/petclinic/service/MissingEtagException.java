package io.micronaut.samples.petclinic.service;

/**
 * Signals that a service operation did not receive its required ETag.
 */
final class MissingEtagException extends Exception {

    MissingEtagException() {
        super("Provide the current ETag.");
    }
}
