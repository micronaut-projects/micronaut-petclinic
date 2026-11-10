package io.micronaut.samples.petclinic.service;

/** Raised when an uploaded pet photo exceeds the configured size limit. */
public class PetPhotoTooLargeException extends RuntimeException {
    public PetPhotoTooLargeException() {
        super("The pet photo exceeds the configured size limit");
    }
}
