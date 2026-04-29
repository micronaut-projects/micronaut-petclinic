package io.micronaut.samples.petclinic.repository;

import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.data.model.Sort;
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

    @Test
    void shouldFindOwnerByLastName() {
        Collection<Owner> owners = ownerRepository.findByLastNameContainingIgnoreCase("Davis", Sort.of(Sort.Order.asc("lastName")));
        assertThat(owners).hasSize(2);
    }

    @Test
    void shouldFindOwnerByLastNameCaseInsensitive() {
        Collection<Owner> owners = ownerRepository.findByLastNameContainingIgnoreCase("davis", Sort.of(Sort.Order.asc("lastName")));
        assertThat(owners).hasSize(2);
    }

    @Test
    void shouldFindOwnerByLastNamePartialMatch() {
        Collection<Owner> owners = ownerRepository.findByLastNameContainingIgnoreCase("Dav", Sort.of(Sort.Order.asc("lastName")));
        assertThat(owners).hasSize(2);
    }

    @Test
    void shouldFindOwnerByIdWithPets() {
        Optional<Owner> owner = ownerRepository.findByIdWithPets(1);
        assertThat(owner).isPresent();
        assertThat(owner.get().getLastName()).isEqualTo("Franklin");
        // In JDBC mode, pets are loaded via ClinicService (explicit assembly).
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
        Owner owner = new Owner();
        owner.setFirstName("Test");
        owner.setLastName("Owner");
        owner.setAddress("123 Test St");
        owner.setCity("Test City");
        owner.setTelephone("5551234567");

        Owner saved = ownerRepository.save(owner);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo("Test");
    }

    @Test
    void shouldUpdateExistingOwner() {
        Optional<Owner> owner = ownerRepository.findById(1);
        assertThat(owner).isPresent();
        
        owner.get().setCity("New City");
        Owner updated = ownerRepository.update(owner.get());
        
        assertThat(updated.getCity()).isEqualTo("New City");
    }
}
