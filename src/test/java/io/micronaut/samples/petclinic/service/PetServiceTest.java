package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Integration coverage for pet and pet-type operations. */
@MicronautTest
class PetServiceTest {
    @Inject
    PetService petService;

    @Inject
    ClinicServiceFixtures clinicServiceFixtures;

    @Test
    void shouldRejectLoadingAPetWithoutAPhoto() {
        Pet pet = clinicServiceFixtures.requiredPet("George", "Franklin", "Leo").withPhotoFilename(null);
        assertThrows(NotFoundEntityException.class, () -> petService.loadPetPhoto(pet));
    }

    @Test
    void shouldFindPetTypes() {
        Collection<PetType> petTypes = petService.findPetTypes();
        assertThat(petTypes).isNotEmpty();
        assertThat(petTypes.size()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void shouldFindById() throws NotFoundEntityException {
        Pet leo = clinicServiceFixtures.requiredPet("George", "Franklin", "Leo");
        Pet found = petService.findById(leo.id());
        assertThat(found.name()).isEqualTo("Leo");
    }

    @Test
    void shouldSaveNewPet() {
        Owner owner = clinicServiceFixtures.requiredOwner("George", "Franklin");
        PetType catType = petService.findPetTypes().stream()
                .filter(t -> t.name().equals("cat"))
                .findFirst()
                .orElseThrow();

        Pet pet = new Pet("Whiskers", LocalDate.of(2023, 1, 1), catType, owner);
        Pet savedPet = petService.savePet(pet);

        assertThat(savedPet.id()).isNotNull();
        assertThat(savedPet.name()).isEqualTo("Whiskers");
    }

}
