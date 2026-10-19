package io.micronaut.samples.petclinic.service;

/**
 * Signals that a service operation violates a domain validation rule.
 */
class DomainValidationException extends Exception {

    /**
     * Creates a domain validation failure.
     *
     * @param message explanation of the violated rule
     */
    public DomainValidationException(String message) {
        super(message);
    }
}
