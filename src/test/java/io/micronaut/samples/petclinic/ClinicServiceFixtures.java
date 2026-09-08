package io.micronaut.samples.petclinic;

import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.service.ClinicService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public final class ClinicServiceFixtures {

    @Inject
    ClinicService clinicService;

    public Optional<Owner> owner(String firstName, String lastName) {
        return clinicService.findOwnerByLastName(lastName).stream()
                .filter(owner -> owner.getFirstName().equals(firstName))
                .findFirst();
    }

    public Owner requiredOwner(String firstName, String lastName) {
        return owner(firstName, lastName)
                .orElseThrow(() -> new AssertionError("Missing owner: " + firstName + " " + lastName));
    }

    public Pet requiredPet(String ownerFirstName, String ownerLastName, String petName) {
        Pet pet = requiredOwner(ownerFirstName, ownerLastName).getPet(petName);
        if (pet == null) {
            throw new AssertionError("Missing pet: " + petName);
        }
        return pet;
    }
}
