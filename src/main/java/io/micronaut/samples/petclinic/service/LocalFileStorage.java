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
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.exception.FileTooLargeException;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stores files under generated keys outside the application classpath on the local filesystem. */
@Singleton
public class LocalFileStorage implements FileStorage {

    private static final Pattern STORED_FILENAME = Pattern.compile("[0-9a-f]{32}\\.[a-z0-9-]{1,64}");
    private final Path storageDirectory;
    private final long maxSizeBytes;

    /**
     * Creates local storage with a bounded read size.
     *
     * @param storageDirectory directory for generated files, relative to the working directory or absolute
     * @param maxSizeBytes maximum number of bytes to read for one stored file
     * @throws IllegalArgumentException if the byte limit is not between 1 and 2147483646
     */
    public LocalFileStorage(
            @Value("${petclinic.photos.storage-dir:./var/pet-photos}") String storageDirectory,
            @Value("${petclinic.photos.max-size-bytes:5242880}") long maxSizeBytes) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        if (maxSizeBytes < 1 || maxSizeBytes >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("File storage max size must be between 1 and 2147483646 bytes");
        }
        this.maxSizeBytes = maxSizeBytes;
    }

    /**
     * {@inheritDoc}
     * Creates the directory when necessary and removes partial files if writing fails.
     */
    @Override
    public String store(byte[] bytes, MediaType mediaType) throws IOException, IllegalRequestArgumentException {
        if (bytes.length > maxSizeBytes) {
            throw new FileTooLargeException();
        }
        Files.createDirectories(storageDirectory);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + mediaType.getExtension();
        Path destination = safePath(filename);
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
     * Reads only a generated filename from the configured directory.
     *
     * @param filename the generated storage key
     * @return the file bytes and the media type inferred from its filename
     * @throws NotFoundEntityException if the file is absent or not a regular file
     * @throws IllegalRequestArgumentException if the key or stored file is invalid
     * @throws IOException if the file cannot be read
     */
    @Override
    public StoredFile load(String filename)
            throws IOException, NotFoundEntityException, IllegalRequestArgumentException {
        Path path = safePath(filename);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new NotFoundEntityException("Stored file not found.");
        }
        MediaType mediaType = MediaType.forFilename(path.getFileName().toString());
        byte[] bytes;
        // Check again when opening, so a file replaced with a symlink cannot escape the directory.
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readNBytes((int) maxSizeBytes + 1);
        } catch (NoSuchFileException removedDuringReplacement) {
            throw new NotFoundEntityException("Stored file not found.");
        }
        if (bytes.length > maxSizeBytes) {
            throw new FileTooLargeException();
        }
        return new StoredFile(bytes, mediaType);
    }

    /**
     * Deletes a previously generated filename. Missing files are ignored.
     *
     * @param filename the generated storage key
     * @throws IllegalRequestArgumentException if the filename is not a valid local storage key
     * @throws IOException if a valid stored file cannot be deleted
     */
    @Override
    public void delete(String filename) throws IOException, IllegalRequestArgumentException {
        Files.deleteIfExists(safePath(filename));
    }

    /**
     * Resolves a generated filename within the configured directory.
     *
     * @param filename the local storage key
     * @return a path directly inside the storage directory
     * @throws IllegalRequestArgumentException if the key is invalid or escapes the directory
     */
    private Path safePath(String filename) throws IllegalRequestArgumentException {
        if (filename == null || !STORED_FILENAME.matcher(filename).matches()) {
            throw new IllegalRequestArgumentException("Invalid file storage key.");
        }
        Path path = storageDirectory.resolve(filename).normalize();
        if (!path.getParent().equals(storageDirectory)) {
            throw new IllegalRequestArgumentException("Invalid file storage key.");
        }
        return path;
    }

}
