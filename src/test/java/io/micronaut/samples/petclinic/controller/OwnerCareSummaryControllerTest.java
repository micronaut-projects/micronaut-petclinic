package io.micronaut.samples.petclinic.controller;

import io.micronaut.cache.CacheManager;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(transactional = false)
@Property(name = "micronaut.http.client.exception-on-error-status", value = "false")
class OwnerCareSummaryControllerTest {

    @Inject @Client("/") HttpClient client;
    @Inject ClinicService clinicService;
    @Inject ClinicServiceFixtures fixtures;
    @Inject CacheManager<?> cacheManager;

    private Owner testOwner;
    private Pet testPet;

    @AfterEach
    void deleteTestData() {
        if (testPet != null) {
            clinicService.deletePet(testPet.id());
        }
        if (testOwner != null) {
            clinicService.deleteOwner(testOwner);
            cacheManager.getCache("owner-details").invalidate(testOwner.id());
        }
    }

    @Test
    void downloadsGeneratedRecordWithOwnerPetsVisitsAndDates() throws NotFoundEntityException {
        Owner owner = clinicService.findOwnerById(fixtures.requiredOwner("Jean", "Coleman").id());
        HttpResponse<Map<String, Object>> response = download(owner.id());

        assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"owner-" + owner.id() + "-care-summary.json\"");

        Map<String, Object> body = Objects.requireNonNull(response.body());
        assertThat(body).containsEntry("_id", owner.id())
                .containsEntry("firstName", owner.firstName())
                .containsEntry("lastName", owner.lastName())
                .containsEntry("address", owner.address())
                .containsEntry("city", owner.city())
                .containsEntry("telephone", owner.telephone())
                .doesNotContainKey("_metadata");
        List<Map<String, Object>> pets = listOfMaps(body.get("pets"));
        assertThat(pets).hasSize(owner.pets().size());
        assertThat(owner.pets()).anyMatch(pet -> !pet.visits().isEmpty());
        for (Pet expectedPet : owner.pets()) {
            Map<String, Object> pet = pets.stream()
                    .filter(value -> Objects.equals(value.get("id"), expectedPet.id()))
                    .findFirst().orElseThrow();
            assertThat(pet).containsEntry("name", expectedPet.name());
            assertThat(pet.get("birthDate")).isEqualTo(expectedPet.birthDate().toString());

            List<Map<String, Object>> visits = listOfMaps(pet.get("visits"));
            assertThat(visits).hasSize(expectedPet.visits().size());
            for (Visit expectedVisit : expectedPet.visits()) {
                Map<String, Object> visit = visits.stream()
                        .filter(value -> Objects.equals(value.get("id"), expectedVisit.id()))
                        .findFirst().orElseThrow();
                assertThat(visit).containsEntry("description", expectedVisit.description());
                assertThat(visit.get("date")).isEqualTo(expectedVisit.date().toString());
            }
        }
    }

    @Test
    void preservesRequiredEmptyCollectionsAndNullableTelephone() {
        testOwner = clinicService.saveOwner(new Owner(
                "Morgan", "NoPets", "100 Example Way", "Madison", null));

        HttpResponse<Map<String, Object>> withoutPets = download(testOwner.id());

        assertThat(withoutPets.code()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(withoutPets.body()).containsEntry("_id", testOwner.id()).containsEntry("pets", List.of());
        assertThat(withoutPets.body().get("telephone")).isNull();
    }

    @Test
    void preservesRequiredEmptyVisitsForAPet() {
        testOwner = clinicService.saveOwner(new Owner(
                "Morgan", "NoVisits", "100 Example Way", "Madison", null));

        testPet = clinicService.savePet(new Pet("Mittens", LocalDate.of(2012, 9, 4),
                clinicService.findPetTypes().getFirst(), testOwner));
        HttpResponse<Map<String, Object>> withoutVisits = download(testOwner.id());

        assertThat(withoutVisits.code()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(listOfMaps(withoutVisits.body().get("pets"))).containsExactly(Map.of(
                "id", testPet.id(), "name", "Mittens", "birthDate", "2012-09-04", "visits", List.of()));
    }

    @Test
    void unknownOwnerReturnsNotFound() {
        HttpResponse<Map<String, Object>> response = download(999999999);

        assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
        assertThat(response.body()).containsEntry("message", "Entity not found.");
    }

    private HttpResponse<Map<String, Object>> download(Integer ownerId) {
        Argument<Map<String, Object>> bodyType = Argument.mapOf(String.class, Object.class);
        return client.toBlocking().exchange(
                HttpRequest.GET("/owners/" + ownerId + "/care-summary").accept(MediaType.APPLICATION_JSON_TYPE),
                bodyType, bodyType);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOfMaps(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
