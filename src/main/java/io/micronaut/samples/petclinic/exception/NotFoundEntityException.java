package io.micronaut.samples.petclinic.exception;

public class NotFoundEntityException extends Exception {


    public NotFoundEntityException() {
        super("Entity not found.");
    }

    public NotFoundEntityException(String message) {
        super(message);
    }
}
