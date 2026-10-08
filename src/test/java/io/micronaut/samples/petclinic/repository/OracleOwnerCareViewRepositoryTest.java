package io.micronaut.samples.petclinic.repository;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.exceptions.OptimisticLockException;
import io.micronaut.data.model.Sort;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.OwnerCareView;
import io.micronaut.samples.petclinic.model.OwnerCareView.PetCareSubView;
import io.micronaut.samples.petclinic.model.OwnerCareView.VisitCareSubView;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.service.OwnerCareViewService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Oracle integration coverage for Micronaut Data's generated JSON duality view. */
@MicronautTest
@Requires(env = "oracle")
class OracleOwnerCareViewRepositoryTest {

    @Inject OwnerCareViewRepository ownerCareViewRepository;
    @Inject OwnerCareViewService ownerCareViewService;
    @Inject OwnerRepository ownerRepository;
    @Inject PetRepository petRepository;
    @Inject VisitRepository visitRepository;

    @Test
    void readsOwnerPetsAndVisitsAsOneAnnotatedDocument() {
        Owner owner = sampleOwner();

        OwnerCareView document = ownerCareViewRepository.findById(owner.id()).orElseThrow();

        assertThat(document.id()).isEqualTo(owner.id());
        assertThat(document.metadata()).isNotNull();
        assertThat(document.metadata().etag()).isNotBlank();
        assertThat(document.pets()).isNotEmpty();
        assertThat(document.pets()).anySatisfy(pet -> assertThat(pet.visits()).isNotNull());
        assertThat(document.pets().stream().flatMap(pet -> pet.visits().stream()))
                .isNotEmpty()
                .allSatisfy(visit -> {
                    assertThat(visit.id()).isNotNull();
                    assertThat(visit.date()).isNotNull();
                    assertThat(visit.description()).isNotBlank();
                });
    }

    @Test
    void updatesOwnerPetAndVisitRowsThroughTheDocumentRepository() {
        Owner owner = sampleOwner();
        OwnerCareView original = ownerCareViewRepository.findById(owner.id()).orElseThrow();
        PetCareSubView originalPet = original.pets().getFirst();
        VisitCareSubView originalVisit = originalPet.visits().getFirst();

        String updatedCity = original.city() + " JSON";
        String updatedPetName = originalPet.name() + " JSON";
        String updatedVisitDescription = originalVisit.description() + " JSON";
        List<PetCareSubView> updatedPets = original.pets().stream()
                .map(pet -> pet.id().equals(originalPet.id())
                        ? new PetCareSubView(pet.id(), updatedPetName, pet.birthDate(),
                        pet.visits().stream()
                                .map(visit -> visit.id().equals(originalVisit.id())
                                        ? new VisitCareSubView(visit.id(), visit.date(), updatedVisitDescription)
                                        : visit)
                                .toList())
                        : pet)
                .toList();
        OwnerCareView updated = new OwnerCareView(original.id(), original.firstName(), original.lastName(),
                original.address(), updatedCity, original.telephone(), updatedPets, original.metadata());

        OwnerCareView saved = ownerCareViewService.update(owner.id(), updated);

        assertThat(ownerRepository.findById(owner.id()).orElseThrow().city()).isEqualTo(updatedCity);
        assertThat(petRepository.findById(originalPet.id()).orElseThrow().name()).isEqualTo(updatedPetName);
        assertThat(visitRepository.findById(originalVisit.id()).orElseThrow().description())
                .isEqualTo(updatedVisitDescription);
        assertThat(saved.metadata().etag()).isNotEqualTo(original.metadata().etag());
    }

    @Test
    void rejectsRemovingPetsOrVisitsFromAnUpdate() {
        Owner owner = sampleOwner();
        OwnerCareView original = ownerCareViewRepository.findById(owner.id()).orElseThrow();
        OwnerCareView missingPet = new OwnerCareView(original.id(), original.firstName(), original.lastName(),
                original.address(), original.city(), original.telephone(), List.of(), original.metadata());

        assertThatThrownBy(() -> ownerCareViewService.update(owner.id(), missingPet))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pets");

        PetCareSubView pet = original.pets().getFirst();
        OwnerCareView missingVisit = new OwnerCareView(original.id(), original.firstName(), original.lastName(),
                original.address(), original.city(), original.telephone(),
                original.pets().stream()
                        .map(existingPet -> existingPet.id().equals(pet.id())
                                ? new PetCareSubView(pet.id(), pet.name(), pet.birthDate(), List.of())
                                : existingPet)
                        .toList(), original.metadata());

        assertThatThrownBy(() -> ownerCareViewService.update(owner.id(), missingVisit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Visits");
    }

    @Test
    void rejectsAnEtagAfterARelationalUpdateChangesTheDocument() {
        Owner owner = sampleOwner();
        OwnerCareView stale = ownerCareViewRepository.findById(owner.id()).orElseThrow();
        ownerRepository.update(new Owner(owner.id(), owner.firstName(), owner.lastName(), owner.address(),
                owner.city() + " Relational", owner.telephone(), owner.pets()));
        OwnerCareView staleEdit = new OwnerCareView(stale.id(), stale.firstName(), stale.lastName(),
                stale.address(), stale.city() + " JSON", stale.telephone(), stale.pets(), stale.metadata());

        assertThatThrownBy(() -> ownerCareViewService.update(owner.id(), staleEdit))
                .isInstanceOf(OptimisticLockException.class);
    }

    private Owner sampleOwner() {
        Collection<Owner> matches = ownerRepository.findByLastNameContainingIgnoreCase(
                "Coleman", Sort.of(Sort.Order.asc("lastName")));
        return matches.stream().findFirst().orElseThrow();
    }
}
