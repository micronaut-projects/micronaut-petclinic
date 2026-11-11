package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.multipart.MultipartBody;
import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.service.PetService;
import io.micronaut.samples.petclinic.service.FileStorage;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** HTTP coverage for pet photo validation, replacement, and display. */
@MicronautTest(transactional = false)
@Property(name = "micronaut.server.multipart.max-file-size", value = "2048")
class PetPhotoControllerTest {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/6WkAAAAASUVORK5CYII=");

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ClinicServiceFixtures fixtures;

    @Inject
    PetService petService;

    @Inject
    FileStorage fileStorage;

    @BeforeEach
    void clearExistingPhoto() throws Exception {
        Pet pet = fixtures.requiredPet("George", "Franklin", "Leo");
        if (pet.photoFilename() != null) {
            fileStorage.delete(pet.photoFilename());
            petService.savePet(pet.withPhotoFilename(null));
        }
    }

    @Test
    void uploadsAndDisplaysPetPhoto() throws Exception {
        Pet pet = fixtures.requiredPet("George", "Franklin", "Leo");
        String path = photoPath(pet);
        String storedFilename = null;

        try {
            HttpResponse<String> uploadResponse = upload(path, "image/png", "pet.png", PNG);

            assertThat((CharSequence) uploadResponse.getStatus()).isEqualTo(HttpStatus.NO_CONTENT);
            Pet updatedPet = petService.findById(pet.id());
            storedFilename = updatedPet.photoFilename();
            assertThat(storedFilename).matches("[0-9a-f]{32}\\.png");

            HttpResponse<byte[]> photoResponse = client.toBlocking().exchange(
                    HttpRequest.GET(path), byte[].class);
            assertThat(photoResponse.getBody().orElseThrow()).containsExactly(PNG);
            assertThat(photoResponse.getContentType()).contains(MediaType.of("image/png"));
            assertThat(photoResponse.getHeaders().get("X-Content-Type-Options")).isEqualTo("nosniff");

            String ownerPage = client.toBlocking().retrieve(
                    HttpRequest.GET("/owners/" + pet.getOwnerId()), String.class);
            assertThat(ownerPage).contains("src=\"" + path + "\"");
        } finally {
            Pet current = petService.findById(pet.id());
            if (current.photoFilename() != null) {
                fileStorage.delete(current.photoFilename());
                petService.savePet(current.withPhotoFilename(null));
            } else if (storedFilename != null) {
                fileStorage.delete(storedFilename);
            }
        }
    }

    @Test
    void rejectsNonImageUploadEvenWhenItClaimsAnImageType() throws Exception {
        Pet pet = fixtures.requiredPet("George", "Franklin", "Leo");
        byte[] text = "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        var failure = assertThrows(HttpClientResponseException.class,
                () -> upload(photoPath(pet), "image/png", "notes.png", text));
        assertThat((CharSequence) failure.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(failure.getResponse().getHeaders().get("Location")).isNull();
        assertThat(failure.getResponse().getBody(String.class).orElseThrow()).contains("invalid_file_format");
        assertThat(petService.findById(pet.id()).photoFilename()).isNull();
    }

    @Test
    void rejectsUnsupportedContentTypeEvenForImageBytes() throws Exception {
        Pet pet = fixtures.requiredPet("George", "Franklin", "Leo");
        var failure = assertThrows(HttpClientResponseException.class,
                () -> upload(photoPath(pet), "text/plain", "pet.png", PNG));
        assertThat((CharSequence) failure.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(failure.getResponse().getBody(String.class).orElseThrow()).contains("invalid_file_format");
        assertThat(petService.findById(pet.id()).photoFilename()).isNull();
    }

    @Test
    void editingStalePetDetailsPreservesTheUploadedPhoto() throws Exception {
        Pet pet = fixtures.requiredPet("George", "Franklin", "Leo");
        try {
            upload(photoPath(pet), "image/png", "pet.png", PNG);
            String filename = petService.findById(pet.id()).photoFilename();
            assertThat(filename).isNotNull();

            petService.updatePetDetails(pet.withName("Edited Leo"));
            Pet current = petService.findById(pet.id());
            assertThat(current.name()).isEqualTo("Edited Leo");
            assertThat(current.photoFilename()).isEqualTo(filename);
            assertThat(petService.loadPetPhoto(current).bytes()).containsExactly(PNG);
        } finally {
            Pet current = petService.findById(pet.id());
            if (current.photoFilename() != null) {
                fileStorage.delete(current.photoFilename());
            }
            petService.savePet(pet.withPhotoFilename(null));
        }
    }

    @Test
    void rejectsUploadAboveConfiguredSizeLimit() throws Exception {
        Pet pet = fixtures.requiredPet("George", "Franklin", "Leo");
        byte[] tooLarge = new byte[1025];
        System.arraycopy(PNG, 0, tooLarge, 0, PNG.length);

        HttpClientResponseException failure = assertThrows(HttpClientResponseException.class,
                () -> upload(photoPath(pet), "image/png", "large.png", tooLarge));

        assertThat((CharSequence) failure.getStatus()).isEqualTo(HttpStatus.REQUEST_ENTITY_TOO_LARGE);
        assertThat(failure.getResponse().getBody(String.class).orElseThrow()).contains("file_too_large");
        assertThat(petService.findById(pet.id()).photoFilename()).isNull();
    }

    @Test
    void frameworkMultipartLimitAlsoReturnsFileTooLarge() throws Exception {
        Pet pet = fixtures.requiredPet("George", "Franklin", "Leo");
        var failure = assertThrows(HttpClientResponseException.class,
                () -> upload(photoPath(pet), "image/png", "large.png", new byte[2049]));
        assertThat((CharSequence) failure.getStatus()).isEqualTo(HttpStatus.REQUEST_ENTITY_TOO_LARGE);
        assertThat(failure.getResponse().getBody(String.class).orElseThrow()).contains("file_too_large");
        assertThat(petService.findById(pet.id()).photoFilename()).isNull();
    }

    @Test
    void replacesPreviousPhotoAndRemovesOldFile() throws Exception {
        Pet pet = fixtures.requiredPet("George", "Franklin", "Leo");
        String path = photoPath(pet);
        String replacementFilename = null;

        try {
            upload(path, "image/png", "first.png", PNG);
            String firstFilename = petService.findById(pet.id()).photoFilename();
            assertThat(firstFilename).isNotNull();

            upload(path, "image/png", "second.png", PNG);
            replacementFilename = petService.findById(pet.id()).photoFilename();

            assertThat(replacementFilename).isNotEqualTo(firstFilename);
            assertThrows(NotFoundEntityException.class, () -> fileStorage.load(firstFilename));
            assertThat(fileStorage.load(replacementFilename)).isNotNull();
        } finally {
            Pet current = petService.findById(pet.id());
            if (current.photoFilename() != null) {
                fileStorage.delete(current.photoFilename());
                petService.savePet(current.withPhotoFilename(null));
            } else if (replacementFilename != null) {
                fileStorage.delete(replacementFilename);
            }
        }
    }

    private HttpResponse<String> upload(String path, String contentType, String filename, byte[] bytes) {
        MultipartBody requestBody = MultipartBody.builder()
                .addPart("photo", filename, MediaType.of(contentType), bytes)
                .build();
        return client.toBlocking().exchange(
                HttpRequest.POST(path, requestBody)
                        .contentType(MediaType.MULTIPART_FORM_DATA_TYPE)
                        .accept(MediaType.ALL_TYPE), String.class);
    }

    private static String photoPath(Pet pet) {
        return "/owners/" + pet.getOwnerId() + "/pets/" + pet.id() + "/photo";
    }

}
