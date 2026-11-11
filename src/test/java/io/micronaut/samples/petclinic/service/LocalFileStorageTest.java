package io.micronaut.samples.petclinic.service;

import org.junit.jupiter.api.Test;
import io.micronaut.http.MediaType;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Local file roundtrip and storage-key safety coverage. */
class LocalFileStorageTest {
    @TempDir
    Path directory;

    @Test
    void storesAndLoadsNonImageFiles() throws Exception {
        FileStorage storage = new LocalFileStorage(directory.toString(), 1024);
        byte[] bytes = "{\"name\":\"Leo\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String key = storage.store(bytes, MediaType.APPLICATION_JSON_TYPE);

        FileStorage.StoredFile file = storage.load(key);
        assertThat(key).endsWith(".json");
        assertThat(file.bytes()).containsExactly(bytes);
        assertThat(file.mediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);

        storage.delete(key);
        assertThrows(NotFoundEntityException.class, () -> storage.load(key));
    }

    @Test
    void refusesTraversalAndAbsolutePathsWithoutDeletingTheirTargets() throws Exception {
        Path outside = Files.writeString(directory.resolve("outside.png"), "private file");
        Path photos = Files.createDirectory(directory.resolve("photos"));
        FileStorage storage = new LocalFileStorage(photos.toString(), 1024);

        assertThrows(IllegalRequestArgumentException.class, () -> storage.load("../outside.png"));
        assertThrows(IllegalRequestArgumentException.class, () -> storage.load(outside.toString()));
        assertThrows(IllegalRequestArgumentException.class, () -> storage.delete("../outside.png"));
        assertThrows(IllegalRequestArgumentException.class, () -> storage.delete(outside.toString()));
        assertThat(Files.readString(outside)).isEqualTo("private file");
    }

    @Test
    void refusesSymlinksAndDeletesOnlyTheLink() throws Exception {
        Path target = Files.writeString(directory.resolve("private.png"), "private file");
        Path photos = Files.createDirectory(directory.resolve("photos"));
        String key = "0123456789abcdef0123456789abcdef.png";
        Path link = Files.createSymbolicLink(photos.resolve(key), target);
        FileStorage storage = new LocalFileStorage(photos.toString(), 1024);

        assertThrows(NotFoundEntityException.class, () -> storage.load(key));
        storage.delete(key);
        assertThat(Files.exists(link, java.nio.file.LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.readString(target)).isEqualTo("private file");
    }
}
