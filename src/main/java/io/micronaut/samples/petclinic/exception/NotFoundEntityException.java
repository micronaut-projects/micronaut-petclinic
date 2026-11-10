package io.micronaut.samples.petclinic.exception;

/** Signals that a requested domain entity does not exist. */
public class NotFoundEntityException extends IllegalRequestArgumentException {

    /** Creates an entity-not-found failure with the default message. */
    public NotFoundEntityException() {
        super("Entity not found.");
    }

    /**
     * Creates an entity-not-found failure with a specific message.
     *
     * @param message description of the missing entity
     */
    public NotFoundEntityException(String message) {
        super(message);
    }
}
