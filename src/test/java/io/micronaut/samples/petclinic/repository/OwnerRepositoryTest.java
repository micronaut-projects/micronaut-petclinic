package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.model.Sort;
import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OwnerRepository}.
 */
@MicronautTest
class OwnerRepositoryTest {

    @Inject
    OwnerRepository ownerRepository;

    @Inject
    ClinicServiceFixtures clinicServiceFixtures;

    @Test
    void shouldFindOwnerByLastName() {
        Collection<Owner> owners = ownerRepository.findByLastNameContainsIgnoreCase("Davis", Sort.of(Sort.Order.asc("lastName")));
        assertThat(owners).hasSize(2);
    }

    @Test
    void shouldFindOwnerByLastNameCaseInsensitive() {
        Collection<Owner> owners = ownerRepository.findByLastNameContainsIgnoreCase("davis", Sort.of(Sort.Order.asc("lastName")));
        assertThat(owners).hasSize(2);
    }

    @Test
    void shouldFindOwnerByLastNamePartialMatch() {
        Collection<Owner> owners = ownerRepository.findByLastNameContainsIgnoreCase("Dav", Sort.of(Sort.Order.asc("lastName")));
        assertThat(owners).hasSize(2);
    }

    @Test
    void shouldFindOwnerByIdWithPets() {
        Owner owner = clinicServiceFixtures.requiredOwner("George", "Franklin");

        Optional<Owner> found = ownerRepository.findByIdWithPets(owner.id());
        assertThat(found).isPresent();
        assertThat(found.get().getLastName()).isEqualTo("Franklin");
        // In JDBC mode, pets are loaded via ClinicService (explicit assembly).
        assertThat(found.get().pets()).isNotEmpty();
    }

    @Test
    void shouldReturnEmptyForNonExistentOwner() {
        Optional<Owner> owner = ownerRepository.findByIdWithPets(999);
        assertThat(owner).isEmpty();
    }

    @Test
    void shouldFindAllOwnersWithPets() {
        Collection<Owner> owners = ownerRepository.findAll(Sort.of(Sort.Order.asc("lastName")));
        assertThat(owners).isNotEmpty();
        assertThat(owners.size()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void shouldSaveNewOwner() {
        Owner owner = new Owner("Test", "Owner", "123 Test St", "Test City", "5551234567");

        Owner saved = ownerRepository.save(owner);
        
        assertThat(saved.id()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("Test");
    }

    @Test
    void shouldUpdateExistingOwner() {
        Owner owner = clinicServiceFixtures.requiredOwner("George", "Franklin");

        Owner updated = ownerRepository.update(owner.withCity("New City"));
        
        assertThat(updated.getCity()).isEqualTo("New City");
    }
}
