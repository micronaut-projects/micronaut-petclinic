package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.model.*;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
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

    @Test
    void shouldFindOwnersByLastName() {
        Collection<Owner> owners = clinicService.findOwnerByLastName("Davis");
        assertThat(owners).hasSize(2);
    }

    @Test
    void shouldFindOwnerById() {
        Optional<Owner> owner = clinicService.findOwnerById(1);
        assertThat(owner).isPresent();
        assertThat(owner.get().getLastName()).isEqualTo("Franklin");
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
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setAddress("123 Main St");
        owner.setCity("Springfield");
        owner.setTelephone("1234567890");
        
        Owner savedOwner = clinicService.saveOwner(owner);
        
        assertThat(savedOwner.getId()).isNotNull();
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
    void shouldFindVetWithSpecialties() {
        Collection<Vet> vets = clinicService.findAllVets();
        Vet vetWithSpecialties = vets.stream()
                .filter(v -> !v.getSpecialties().isEmpty())
                .findFirst()
                .orElse(null);
        
        assertThat(vetWithSpecialties).isNotNull();
        assertThat(vetWithSpecialties.getSpecialties()).isNotEmpty();
    }

    @Test
    void shouldFindPetById() {
        Optional<Pet> pet = clinicService.findPetById(1);
        assertThat(pet).isPresent();
        assertThat(pet.get().getName()).isEqualTo("Leo");
    }

    @Test
    void shouldSaveNewPet() {
        Optional<Owner> owner = clinicService.findOwnerById(1);
        assertThat(owner).isPresent();
        
        PetType catType = clinicService.findPetTypes().stream()
                .filter(t -> t.getName().equals("cat"))
                .findFirst()
                .orElseThrow();
        
        Pet pet = new Pet();
        pet.setName("Whiskers");
        pet.setBirthDate(LocalDate.of(2023, 1, 1));
        pet.setType(catType);
        pet.setOwner(owner.get());
        
        Pet savedPet = clinicService.savePet(pet);
        
        assertThat(savedPet.getId()).isNotNull();
        assertThat(savedPet.getName()).isEqualTo("Whiskers");
    }

    @Test
    void shouldSaveNewVisit() {
        Optional<Pet> pet = clinicService.findPetById(1);
        assertThat(pet).isPresent();
        
        Visit visit = new Visit();
        visit.setDate(LocalDate.now());
        visit.setDescription("Annual checkup");
        visit.setPet(pet.get());
        
        Visit savedVisit = clinicService.saveVisit(visit);
        
        assertThat(savedVisit.getId()).isNotNull();
        assertThat(savedVisit.getDescription()).isEqualTo("Annual checkup");
    }

    @Test
    void shouldFindVisitsByPetId() {
        // Pet with ID 7 (Samantha) has visits in sample data
        Collection<Visit> visits = clinicService.findVisitsByPetId(7);
        assertThat(visits).isNotEmpty();
    }

    @Test
    void shouldFindAllSpecialties() {
        Collection<Specialty> specialties = clinicService.findAllSpecialties();
        assertThat(specialties).isNotEmpty();
        assertThat(specialties.size()).isGreaterThanOrEqualTo(3);
    }
}
