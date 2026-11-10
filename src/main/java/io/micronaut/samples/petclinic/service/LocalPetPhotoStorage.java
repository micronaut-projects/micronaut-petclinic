package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.MediaType;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stores generated pet photo files outside the application classpath on the local filesystem. */
@Singleton
public class LocalPetPhotoStorage implements PetPhotoStorage {

    private static final Pattern STORED_FILENAME = Pattern.compile("[0-9a-f]{32}\\.(jpg|png|gif)");
    private final Path storageDirectory;
    private final long maxSizeBytes;

    public LocalPetPhotoStorage(
            @Value("${petclinic.photos.storage-dir:./var/pet-photos}") String storageDirectory,
            @Value("${petclinic.photos.max-size-bytes:5242880}") long maxSizeBytes) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        if (maxSizeBytes < 1 || maxSizeBytes >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Pet photo max size must be between 1 and 2147483646 bytes");
        }
        this.maxSizeBytes = maxSizeBytes;
    }

    /** Stores validated image bytes under a generated local key. */
    @Override
    public String store(PetPhotoValidator.ValidatedPhoto photo) throws IOException {
        byte[] bytes = photo.bytes();
        Files.createDirectories(storageDirectory);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + photo.extension();
        Path destination = safePath(filename).orElseThrow(InvalidPetPhotoException::new);
        // CREATE_NEW prevents overwriting an existing file or following a planted symlink.
        var output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try (output) {
            output.write(bytes);
        } catch (IOException failure) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        return filename;
    }

    /**
     * Reads only a generated photo filename from the configured directory.
     *
     * @param filename metadata persisted on the pet
     * @return the image bytes and verified media type, if the file exists
     * @throws IOException if the file cannot be read
     */
    @Override
    public Optional<StoredPhoto> load(String filename) throws IOException {
        Optional<Path> safePath = safePath(filename);
        if (safePath.isEmpty()) {
            return Optional.empty();
        }
        Path path = safePath.get();
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        MediaType imageType = mediaType(filename).orElse(null);
        if (imageType == null) {
            return Optional.empty();
        }
        byte[] bytes;
        // Check again when opening, so a file replaced with a symlink cannot escape the directory.
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readNBytes((int) maxSizeBytes + 1);
        } catch (NoSuchFileException removedDuringReplacement) {
            return Optional.empty();
        }
        if (bytes.length > maxSizeBytes) {
            return Optional.empty();
        }
        return Optional.of(new StoredPhoto(bytes, imageType));
    }

    /**
     * Deletes a previously generated filename. Invalid metadata is ignored.
     *
     * @param filename generated filename persisted on a pet
     * @throws IOException if a valid stored file cannot be deleted
     */
    @Override
    public void delete(String filename) throws IOException {
        Optional<Path> path = safePath(filename);
        if (path.isPresent()) {
            Files.deleteIfExists(path.get());
        }
    }

    private Optional<Path> safePath(String filename) {
        if (filename == null || !STORED_FILENAME.matcher(filename).matches()) {
            return Optional.empty();
        }
        Path path = storageDirectory.resolve(filename).normalize();
        return path.getParent().equals(storageDirectory) ? Optional.of(path) : Optional.empty();
    }

    private static Optional<MediaType> mediaType(String filename) {
        if (filename.endsWith(".jpg")) return Optional.of(MediaType.of("image/jpeg"));
        if (filename.endsWith(".png")) return Optional.of(MediaType.of("image/png"));
        if (filename.endsWith(".gif")) return Optional.of(MediaType.of("image/gif"));
        return Optional.empty();
    }
}
