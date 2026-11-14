package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.exception.FileTooLargeException;
import io.micronaut.samples.petclinic.exception.InvalidFileFormatException;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/** Shared upload validation, independent of the selected storage backend. */
@Singleton
public class PetPhotoValidator {
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.IMAGE_GIF);
    private final int maxSizeBytes;

    /**
     * Creates a validator for JPEG, PNG, and GIF uploads.
     *
     * @param maxSizeBytes maximum encoded image size in bytes
     * @throws IllegalArgumentException if the byte limit is nonpositive or exceeds 2147483646
     */
    public PetPhotoValidator(
            @Value("${petclinic.photos.max-size-bytes:5242880}") long maxSizeBytes) {
        if (maxSizeBytes < 1 || maxSizeBytes >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Photo byte limit must be positive and below 2147483647");
        }
        this.maxSizeBytes = (int) maxSizeBytes;
    }

    /**
     * Reads a bounded multipart upload and checks its declared content type and size.
     * The caller remains responsible for closing the upload.
     *
     * @param upload the completed multipart file
     * @return validated image data suitable for any storage implementation
     * @throws IOException if reading the upload fails
     * @throws InvalidFileFormatException if the declared content type is unsupported
     * @throws FileTooLargeException if the encoded image exceeds the byte limit
     */
    public ValidatedPhoto validate(CompletedFileUpload upload) throws IOException, InvalidFileFormatException, FileTooLargeException {
        if (upload.getSize() > maxSizeBytes) {
            throw new FileTooLargeException();
        }
        String declaredType = upload.getContentType().map(MediaType::getName).orElse("");
        byte[] bytes;
        try (var input = upload.getInputStream()) {
            bytes = input.readNBytes(maxSizeBytes + 1);
        }
        return validate(bytes, declaredType);
    }

    /**
     * Checks the byte limit and declared JPEG, PNG, or GIF content type.
     * Does not inspect or decode the file contents.
     *
     * @param bytes the encoded image data
     * @param declaredType the declared JPEG, PNG, or GIF MIME type
     * @return a validated snapshot independent of the supplied array
     * @throws InvalidFileFormatException if the declared content type is unsupported
     * @throws FileTooLargeException if the encoded image exceeds the byte limit
     */
    public ValidatedPhoto validate(byte[] bytes, String declaredType) throws InvalidFileFormatException, FileTooLargeException {
        if (bytes.length > maxSizeBytes) {
            throw new FileTooLargeException();
        }
        String type = declaredType == null ? "" : declaredType.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(type)) {
            throw new InvalidFileFormatException();
        }
        return new ValidatedPhoto(bytes.clone(), MediaType.of(type));
    }

    /** Immutable data created only after successful validation. */
    public static final class ValidatedPhoto {
        private final byte[] bytes;
        private final MediaType mediaType;

        /**
         * Creates the validated payload from the validator's owned byte snapshot.
         *
         * @param bytes the owned encoded image bytes
         * @param mediaType the supported declared image content type
         */
        private ValidatedPhoto(byte[] bytes, MediaType mediaType) {
            this.bytes = bytes;
            this.mediaType = mediaType;
        }

        /**
         * Returns a defensive copy of the validated encoded image.
         * @return a new byte array containing the image data
         */
        public byte[] bytes() { return bytes.clone(); }
        /**
         * Returns the supported declared content type.
         * @return the validated image media type
         */
        public MediaType mediaType() { return mediaType; }
        /**
         * Returns the storage extension for the validated format.
         * @return {@code jpeg}, {@code png}, or {@code gif}, without a leading dot
         */
        public String extension() { return mediaType.getExtension(); }
    }
}
