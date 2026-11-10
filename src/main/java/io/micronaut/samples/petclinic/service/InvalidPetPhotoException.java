package io.micronaut.samples.petclinic.service;

/** Raised when an uploaded pet photo is not an accepted raster image. */
public class InvalidPetPhotoException extends RuntimeException {
    public InvalidPetPhotoException() {
        super("Only valid JPEG, PNG, or GIF images are accepted");
    }
}
