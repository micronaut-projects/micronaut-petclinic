package io.micronaut.samples.petclinic.service;

import io.micronaut.http.MediaType;

import java.io.IOException;
import java.util.Optional;

/** Storage contract for pet photos, independent of where the image bytes are persisted. */
public interface PetPhotoStorage {

    /**
     * Stores a validated image, returning its opaque storage key.
     *
     * @param photo validated image data
     * @return opaque key to persist against the pet
     * @throws IOException when the image cannot be read or saved
     */
    String store(PetPhotoValidator.ValidatedPhoto photo) throws IOException;

    /**
     * Loads an image addressed by a stored key.
     *
     * @param key opaque key persisted against the pet
     * @return image bytes and media type when the image exists
     * @throws IOException when the image cannot be read
     */
    Optional<StoredPhoto> load(String key) throws IOException;

    /**
     * Deletes an image addressed by its stored key.
     *
     * @param key opaque key persisted against the pet
     * @throws IOException when the image cannot be deleted
     */
    void delete(String key) throws IOException;

    /** Image bytes and a media type that is safe to return to the browser. */
    record StoredPhoto(byte[] bytes, MediaType mediaType) {
    }
}
