package io.micronaut.samples.petclinic.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalPetPhotoStorageTest {
    @TempDir
    Path directory;

    @Test
    void refusesTraversalAndAbsolutePathsWithoutDeletingTheirTargets() throws Exception {
        Path outside = Files.writeString(directory.resolve("outside.png"), "private file");
        Path photos = Files.createDirectory(directory.resolve("photos"));
        PetPhotoStorage storage = new LocalPetPhotoStorage(photos.toString(), 1024);

        assertThat(storage.load("../outside.png")).isEmpty();
        assertThat(storage.load(outside.toString())).isEmpty();
        storage.delete("../outside.png");
        storage.delete(outside.toString());
        assertThat(Files.readString(outside)).isEqualTo("private file");
    }

    @Test
    void refusesSymlinksAndDeletesOnlyTheLink() throws Exception {
        Path target = Files.writeString(directory.resolve("private.png"), "private file");
        Path photos = Files.createDirectory(directory.resolve("photos"));
        String key = "0123456789abcdef0123456789abcdef.png";
        Path link = Files.createSymbolicLink(photos.resolve(key), target);
        PetPhotoStorage storage = new LocalPetPhotoStorage(photos.toString(), 1024);

        assertThat(storage.load(key)).isEmpty();
        storage.delete(key);
        assertThat(Files.exists(link, java.nio.file.LinkOption.NOFOLLOW_LINKS)).isFalse();
        assertThat(Files.readString(target)).isEqualTo("private file");
    }
}
