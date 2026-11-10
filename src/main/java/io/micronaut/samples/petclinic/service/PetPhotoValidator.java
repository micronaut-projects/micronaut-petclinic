package io.micronaut.samples.petclinic.service;

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
import java.util.Map;

/** Shared upload validation, independent of the selected storage backend. */
@Singleton
public class PetPhotoValidator {
    private static final Map<String, String> TYPES = Map.of(
            "JPEG", "image/jpeg", "PNG", "image/png", "GIF", "image/gif");
    private final int maxSizeBytes;
    private final int maxDimension;

    public PetPhotoValidator(
            @Value("${petclinic.photos.max-size-bytes:5242880}") long maxSizeBytes,
            @Value("${petclinic.photos.max-dimension:4096}") int maxDimension) {
        if (maxSizeBytes < 1 || maxSizeBytes >= Integer.MAX_VALUE || maxDimension < 1) {
            throw new IllegalArgumentException("Photo validation limits must be positive and the byte limit below 2147483647");
        }
        this.maxSizeBytes = (int) maxSizeBytes;
        this.maxDimension = maxDimension;
    }

    /** Reads a bounded multipart upload and checks that the image can be decoded. */
    public ValidatedPhoto validate(CompletedFileUpload upload) throws IOException {
        if (upload.getSize() > maxSizeBytes) throw new PetPhotoTooLargeException();
        String declaredType = upload.getContentType().map(MediaType::getName).orElse("");
        byte[] bytes;
        try (var input = upload.getInputStream()) {
            bytes = input.readNBytes(maxSizeBytes + 1);
        }
        return validate(bytes, declaredType);
    }

    /** Validates image data supplied by any transport. */
    public ValidatedPhoto validate(byte[] bytes, String declaredType) {
        if (bytes.length > maxSizeBytes) throw new PetPhotoTooLargeException();
        if (declaredType == null || !TYPES.containsValue(declaredType.toLowerCase(Locale.ROOT))) {
            throw new InvalidPetPhotoException();
        }
        // Own a snapshot so caller mutations cannot change data after validation.
        byte[] snapshot = bytes.clone();
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(snapshot))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new InvalidPetPhotoException();
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toUpperCase(Locale.ROOT);
                String mediaType = TYPES.get(format);
                if (mediaType == null || !mediaType.equalsIgnoreCase(declaredType)) {
                    throw new InvalidPetPhotoException();
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > maxDimension || height > maxDimension) {
                    throw new InvalidPetPhotoException();
                }
                var decoded = reader.read(0);
                if (decoded == null) throw new InvalidPetPhotoException();
                decoded.flush();
                return new ValidatedPhoto(snapshot, MediaType.of(mediaType), format.equals("JPEG") ? "jpg" : format.toLowerCase(Locale.ROOT));
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException malformed) {
            throw new InvalidPetPhotoException();
        }
    }

    /** Immutable data created only after successful validation. */
    public static final class ValidatedPhoto {
        private final byte[] bytes;
        private final MediaType mediaType;
        private final String extension;

        private ValidatedPhoto(byte[] bytes, MediaType mediaType, String extension) {
            this.bytes = bytes;
            this.mediaType = mediaType;
            this.extension = extension;
        }

        public byte[] bytes() { return bytes.clone(); }
        public MediaType mediaType() { return mediaType; }
        public String extension() { return extension; }
    }
}
