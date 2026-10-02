package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.dto.VisitSearchCriteria;
import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ClinicService}.
 * Uses the default H2 in-memory database with sample data.
 */
@MicronautTest
class ClinicServiceTest {

    @Inject
    ClinicService clinicService;

    @Inject
    ClinicServiceFixtures clinicServiceFixtures;

    @Test
    void shouldFindOwnersByLastName() {
        Collection<Owner> owners = clinicService.findOwnerByLastName("Davis");
        assertThat(owners).hasSize(2);
    }

    @Test
    void shouldFindOwnerById() {
        Owner owner = clinicServiceFixtures.requiredOwner("George", "Franklin");

        Optional<Owner> found = clinicService.findOwnerById(owner.id());
        assertThat(found).isPresent();
        assertThat(found.get().getLastName()).isEqualTo("Franklin");
    }

    @Test
    void shouldReturnEmptyWhenOwnerNotFound() {
        Optional<Owner> owner = clinicService.findOwnerById(999);
        assertThat(owner).isEmpty();
    }

    @Test
    void shouldFindAllOwners() {
        Collection<Owner> owners = clinicService.findAllOwners();
        assertThat(owners).isNotEmpty();
        assertThat(owners.size()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void shouldSaveNewOwner() {
        Owner owner = new Owner("John", "Doe", "123 Main St", "Springfield", "1234567890");
        
        Owner savedOwner = clinicService.saveOwner(owner);
        
        assertThat(savedOwner.id()).isNotNull();
        assertThat(savedOwner.getFirstName()).isEqualTo("John");
    }

    @Test
    void shouldFindPetTypes() {
        Collection<PetType> petTypes = clinicService.findPetTypes();
        assertThat(petTypes).isNotEmpty();
        assertThat(petTypes.size()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void shouldFindAllVets() {
        Collection<Vet> vets = clinicService.findAllVets();
        assertThat(vets).isNotEmpty();
        assertThat(vets.size()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void shouldFindVetWithSpecialities() {
        Collection<Vet> vets = clinicService.findAllVets();
        Vet vetWithSpecialities = vets.stream()
                .filter(v -> !v.getSpecialities().isEmpty())
                .findFirst()
                .orElse(null);

        assertThat(vetWithSpecialities).isNotNull();
        assertThat(vetWithSpecialities.getSpecialities()).isNotEmpty();
    }

    @Test
    void shouldFindPetById() {
        Pet leo = clinicServiceFixtures.requiredPet("George", "Franklin", "Leo");
        Optional<Pet> found = clinicService.findPetById(leo.id());
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Leo");
    }

    @Test
    void shouldSaveNewPet() {
        Owner owner = clinicServiceFixtures.requiredOwner("George", "Franklin");
        PetType catType = clinicService.findPetTypes().stream()
                .filter(t -> t.name().equals("cat"))
                .findFirst()
                .orElseThrow();

        Pet pet = new Pet("Whiskers", LocalDate.of(2023, 1, 1), catType, owner);
        Pet savedPet = clinicService.savePet(pet);
        
        assertThat(savedPet.id()).isNotNull();
        assertThat(savedPet.name()).isEqualTo("Whiskers");
    }

    @Test
    void shouldSaveNewVisit() {
        Pet leo = clinicServiceFixtures.requiredPet("George", "Franklin", "Leo");
        Visit visit = new Visit(LocalDate.now(), "Annual checkup", leo);
        Visit savedVisit = clinicService.saveVisit(visit);

        assertThat(savedVisit.id()).isNotNull();
        assertThat(savedVisit.description()).isEqualTo("Annual checkup");
    }

    @Test
    void shouldFindVisitsByPetId() {
        Pet samantha = clinicServiceFixtures.requiredPet("Jean", "Coleman", "Samantha");
        Collection<Visit> visits = clinicService.findVisitsByPetId(samantha.id());
        assertThat(visits).isNotEmpty();
    }

    @Test
    void shouldSearchVisitsByDurationAndFollowUpPeriod() {
        List<Visit> visits = clinicService.searchVisits(new VisitSearchCriteria(
                LocalDate.of(1900, 1, 1),
                LocalDate.of(9999, 12, 31),
                61,
                13
        ));

        assertThat(visits).isNotEmpty();
        assertThat(visits).allSatisfy(visit -> {
            assertThat(visit.duration().toMinutes()).isLessThanOrEqualTo(61);
            assertThat(visit.period().toTotalMonths()).isLessThanOrEqualTo(13);
        });
    }

    @Test
    void shouldFindAllSpecialities() {
        Collection<Speciality> specialities = clinicService.findAllSpecialities();
        assertThat(specialities).isNotEmpty();
        assertThat(specialities.size()).isGreaterThanOrEqualTo(3);
    }
}
