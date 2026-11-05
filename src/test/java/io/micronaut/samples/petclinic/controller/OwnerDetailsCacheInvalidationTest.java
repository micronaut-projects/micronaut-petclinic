package io.micronaut.samples.petclinic.controller;

import io.micronaut.cache.CacheManager;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.samples.petclinic.dto.VisitForm;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies that HTTP writes evict the owner's cache entry with either provider. */
@MicronautTest(transactional = false)
@Property(name = "micronaut.http.client.follow-redirects", value = "false")
class OwnerDetailsCacheInvalidationTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ClinicService clinicService;

    @Inject
    CacheManager<?> cacheManager;

    @Inject
    OwnerRepository ownerRepository;

    private final List<Owner> createdOwners = new ArrayList<>();

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("owner-details").invalidateAll();
    }

    @AfterEach
    void removeTestData() {
        for (Owner owner : createdOwners) {
            ownerRepository.findById(owner.id()).ifPresent(persisted -> {
                for (Pet pet : persisted.getPets()) {
                    for (Visit visit : pet.getVisits()) {
                        clinicService.deleteVisit(visit.id());
                    }
                    clinicService.deletePet(pet.id());
                }
                clinicService.deleteOwner(persisted.withPets(List.of()));
            });
        }
        clearCache();
    }

    @Test
    void ownerEditsEvictOnlyTheOwnerInTheUrl() throws Exception {
        Owner owner = createOwner("First");
        Owner unrelated = createOwner("Unrelated");
        cachedOwner(owner.id());
        cachedOwner(unrelated.id());

        HttpResponse<?> response = submitForm("/owners/" + owner.id() + "/edit", Map.of(
                "firstName", owner.firstName(), "lastName", owner.lastName(),
                "address", "Updated address", "city", owner.city(), "telephone", owner.telephone()));

        assertRedirect(response);
        assertInvalidated(owner.id());
        assertStillCached(unrelated.id());
        assertThat(cachedOwner(owner.id()).address()).isEqualTo("Updated address");
    }

    @Test
    void petCreationAndEditsEvictOnlyTheOwnerInTheUrl() throws Exception {
        Owner owner = createOwner("Pet");
        Owner unrelated = createOwner("Unrelated");
        Integer typeId = clinicService.findPetTypes().getFirst().id();
        assertThat(cachedOwner(owner.id()).getPets()).isEmpty();
        cachedOwner(unrelated.id());

        assertRedirect(submitForm("/owners/" + owner.id() + "/pets/new", petForm("Cache pet", typeId)));
        assertInvalidated(owner.id());
        assertStillCached(unrelated.id());
        Owner withPet = cachedOwner(owner.id());
        assertThat(withPet.getPets()).extracting(Pet::name).containsExactly("Cache pet");

        Integer petId = withPet.getPets().getFirst().id();
        assertRedirect(submitForm("/owners/" + owner.id() + "/pets/" + petId + "/edit",
                petForm("Updated pet", typeId)));
        assertInvalidated(owner.id());
        assertStillCached(unrelated.id());
        assertThat(cachedOwner(owner.id()).getPets()).extracting(Pet::name).containsExactly("Updated pet");
    }

    @Test
    void visitCreationEvictsOnlyTheOwnerInTheUrl() throws Exception {
        Owner owner = createOwner("Visit");
        Owner unrelated = createOwner("Unrelated");
        Pet pet = createPet(owner);
        assertThat(cachedOwner(owner.id()).getPets().getFirst().getVisits()).isEmpty();
        cachedOwner(unrelated.id());

        HttpResponse<?> response = submitVisit(owner.id(), pet.id());

        assertRedirect(response);
        assertInvalidated(owner.id());
        assertStillCached(unrelated.id());
        assertThat(cachedOwner(owner.id()).getPets().getFirst().getVisits())
                .extracting(Visit::description).containsExactly("New visit");
    }

    @Test
    void invalidPetFormsRetainCachedDetails() throws Exception {
        Owner owner = createOwner("Invalid");
        cachedOwner(owner.id());

        HttpResponse<?> response = submitForm("/owners/" + owner.id() + "/pets/new",
                petForm("", clinicService.findPetTypes().getFirst().id()));

        assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
        assertStillCached(owner.id());
        assertThat(ownerRepository.findById(owner.id()).orElseThrow().getPets()).isEmpty();
    }

    @Test
    void petEditsCannotChangeAnotherOwnersPet() throws Exception {
        Owner owner = createOwner("Actual");
        Owner other = createOwner("Other");
        Pet pet = createPet(owner);
        cachedOwner(owner.id());
        cachedOwner(other.id());

        HttpResponse<?> response = submitForm("/owners/" + other.id() + "/pets/" + pet.id() + "/edit",
                petForm("Wrong owner", pet.type().id()));

        assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
        assertStillCached(owner.id());
        assertStillCached(other.id());
        assertThat(clinicService.findPetById(pet.id()).getOwnerId()).isEqualTo(owner.id());
    }

    @Test
    void visitSubmissionsForAnotherOwnersPetRetainBothEntries() throws Exception {
        Owner owner = createOwner("Actual");
        Owner other = createOwner("Other");
        Pet pet = createPet(owner);
        cachedOwner(owner.id());
        cachedOwner(other.id());

        HttpResponse<?> response = submitVisit(other.id(), pet.id());

        assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
        assertStillCached(owner.id());
        assertStillCached(other.id());
        assertThat(ownerRepository.findById(owner.id()).orElseThrow().getPets().getFirst().getVisits()).isEmpty();
    }

    private Owner createOwner(String firstName) {
        Owner owner = clinicService.saveOwner(new Owner(
                firstName, "CacheInvalidation", "Original address", "Madison", "1234567890"));
        createdOwners.add(owner);
        return owner;
    }

    private Pet createPet(Owner owner) {
        return clinicService.savePet(new Pet("Cache pet", LocalDate.of(2020, 1, 1),
                clinicService.findPetTypes().getFirst(), owner));
    }

    private Map<String, String> petForm(String name, Integer typeId) {
        return Map.of("name", name, "birthDate", "2020-01-01", "typeId", typeId.toString());
    }

    private HttpResponse<?> submitForm(String uri, Map<String, String> form) {
        return exchange(HttpRequest.POST(uri, form).contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    private HttpResponse<?> submitVisit(Integer ownerId, Integer petId) {
        return exchange(HttpRequest.POST(
                "/owners/" + ownerId + "/pets/" + petId + "/visits/new",
                new VisitForm(LocalDate.of(2025, 1, 1), "New visit", 30, 6))
                .contentType(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON_TYPE));
    }

    private HttpResponse<?> exchange(HttpRequest<?> request) {
        try {
            return client.toBlocking().exchange(request, String.class);
        } catch (HttpClientResponseException exception) {
            return exception.getResponse();
        }
    }

    private void assertRedirect(HttpResponse<?> response) {
        assertThat(response.code()).isEqualTo(HttpStatus.MOVED_PERMANENTLY.getCode());
    }

    private Owner cachedOwner(Integer ownerId) throws NotFoundEntityException {
        Owner owner = clinicService.findOwnerById(ownerId);
        assertThat(cacheManager.getCache("owner-details").get(ownerId, Owner.class)).contains(owner);
        return owner;
    }

    private void assertInvalidated(Integer ownerId) {
        assertThat(cacheManager.getCache("owner-details").get(ownerId, Owner.class)).isEmpty();
    }

    private void assertStillCached(Integer ownerId) {
        assertThat(cacheManager.getCache("owner-details").get(ownerId, Owner.class)).isPresent();
    }
}
