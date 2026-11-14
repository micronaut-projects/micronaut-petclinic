package io.micronaut.samples.petclinic.service;

import io.micronaut.http.MediaType;

import java.io.IOException;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;

/** Storage contract for files, independent of where their bytes are persisted. */
public interface FileStorage {

    /**
     * Stores file content under a new opaque key without overwriting an existing file.
     *
     * @param bytes the file content
     * @param mediaType the content type used to determine the storage extension
     * @return the generated storage key
     * @throws IllegalRequestArgumentException when the file exceeds the size limit or its media type cannot produce a valid key
     * @throws IOException when the file cannot be saved
     */
    String store(byte[] bytes, MediaType mediaType) throws IOException, IllegalRequestArgumentException;

    /**
     * Loads a file addressed by a stored key.
     *
     * @param key the generated storage key
     * @return file bytes and the media type inferred from the stored filename
     * @throws NotFoundEntityException when the stored file does not exist
     * @throws IllegalRequestArgumentException when the key or stored file is invalid
     * @throws IOException when the file cannot be read
     */
    StoredFile load(String key) throws IOException, NotFoundEntityException, IllegalRequestArgumentException;

    /**
     * Deletes a file addressed by its stored key. Missing files are ignored.
     *
     * @param key the generated storage key
     * @throws IllegalRequestArgumentException when the storage key is invalid
     * @throws IOException when the file cannot be deleted
     */
    void delete(String key) throws IOException, IllegalRequestArgumentException;

    /**
     * Stored file content and its associated media type.
     *
     * @param bytes the stored file bytes
     * @param mediaType the file content type
     */
    record StoredFile(byte[] bytes, MediaType mediaType) {
    }
}
