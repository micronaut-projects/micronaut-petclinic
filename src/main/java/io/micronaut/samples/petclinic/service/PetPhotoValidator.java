package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.exception.FileTooLargeException;
import io.micronaut.samples.petclinic.exception.InvalidFileFormatException;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.MediaType;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/** Shared upload validation, independent of the selected storage backend. */
@Singleton
public class PetPhotoValidator {
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.IMAGE_GIF);
    private final int maxSizeBytes;
    private final int maxDimension;

    /**
     * Creates a validator for JPEG, PNG, and GIF uploads.
     *
     * @param maxSizeBytes maximum encoded image size in bytes
     * @param maxDimension maximum width and height in pixels
     * @throws IllegalArgumentException if either limit is nonpositive or the byte limit exceeds 2147483646
     */
    public PetPhotoValidator(
            @Value("${petclinic.photos.max-size-bytes:5242880}") long maxSizeBytes,
            @Value("${petclinic.photos.max-dimension:4096}") int maxDimension) {
        if (maxSizeBytes < 1 || maxSizeBytes >= Integer.MAX_VALUE || maxDimension < 1) {
            throw new IllegalArgumentException("Photo validation limits must be positive and the byte limit below 2147483647");
        }
        this.maxSizeBytes = (int) maxSizeBytes;
        this.maxDimension = maxDimension;
    }

    /**
     * Reads a bounded multipart upload and validates its image content.
     * The caller remains responsible for closing the upload.
     *
     * @param upload the completed multipart file
     * @return validated image data suitable for any storage implementation
     * @throws IOException if reading the upload fails
     * @throws InvalidFileFormatException if the type, image data, or dimensions are invalid
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
     * Checks the declared type and decodes the first image frame after checking its dimensions.
     * Retains the original encoded bytes; GIF animation frames after the first are not decoded.
     *
     * @param bytes the encoded image data
     * @param declaredType the declared JPEG, PNG, or GIF MIME type
     * @return a validated snapshot independent of the supplied array
     * @throws InvalidFileFormatException if the declared type differs from the decoded format,
     *         decoding fails, or the image exceeds the dimension limit
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
        MediaType mediaType = MediaType.of(type);

        // Use the declared format's decoder so mismatched image data is rejected during decoding.
        var readers = ImageIO.getImageReadersByMIMEType(mediaType.getName());
        if (!readers.hasNext()) {
            throw new InvalidFileFormatException();
        }
        ImageReader reader = readers.next();
        byte[] snapshot = bytes.clone();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(snapshot))) {
            reader.setInput(input, true, true);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            if (width < 1 || height < 1 || width > maxDimension || height > maxDimension) {
                throw new InvalidFileFormatException();
            }
            reader.read(0).flush();
            return new ValidatedPhoto(snapshot, mediaType);
        } catch (IOException | IllegalArgumentException malformed) {
            throw new InvalidFileFormatException();
        } finally {
            reader.dispose();
        }
    }

    /** Immutable data created only after successful validation. */
    public static final class ValidatedPhoto {
        private final byte[] bytes;
        private final MediaType mediaType;

        /**
         * Creates the validated payload from the validator's owned byte snapshot.
         *
         * @param bytes the owned encoded image bytes
         * @param mediaType the decoded image content type
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
         * Returns the content type determined during validation.
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
