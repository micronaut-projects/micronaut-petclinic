package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.annotation.Transient;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Builder;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static io.micronaut.data.annotation.Relation.Kind.ONE_TO_MANY;

/**
 * Entity representing a pet owner.
 * An owner can have multiple pets.
 *
 * @param id the database identifier, or {@code null} for a new owner
 * @param firstName the owner's first name
 * @param lastName the owner's last name
 * @param address the owner's street address
 * @param city the owner's city
 * @param telephone the owner's 10-digit telephone number
 * @param pets the owner's pets
 */
@MappedEntity("OWNERS")
@Serdeable
@Builder
@Wither
public record Owner(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("FIRST_NAME")
        @NotBlank
        String firstName,

        @MappedProperty("LAST_NAME")
        @NotBlank
        String lastName,

        @MappedProperty("ADDRESS")
        @NotBlank
        String address,

        @MappedProperty("CITY")
        @NotBlank
        String city,

        @MappedProperty("TELEPHONE")
        @NotBlank
        @Digits(fraction = 0, integer = 10)
        String telephone,

        @Relation(value = ONE_TO_MANY, mappedBy = "owner", cascade = Relation.Cascade.ALL)
        List<Pet> pets
) implements Person, OwnerWither {

    /**
     * Creates an owner and normalizes a {@code null} pet list to an empty immutable list.
     */
    public Owner {
        pets = pets != null ? List.copyOf(pets) : List.of();
    }

    /**
     * Creates an empty owner for framework binding.
     */
    public Owner() {
        this(null, null, null, null, null, null, List.of());
    }

    /**
     * Creates a new owner without an id or pets.
     *
     * @param firstName the owner's first name
     * @param lastName the owner's last name
     * @param address the owner's street address
     * @param city the owner's city
     * @param telephone the owner's 10-digit telephone number
     */
    public Owner(String firstName, String lastName, String address, String city, String telephone) {
        this(null, firstName, lastName, address, city, telephone, List.of());
    }

    /**
     * Returns the owner's street address.
     *
     * @return the street address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the owner's city.
     *
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * Returns the owner's telephone number.
     *
     * @return the telephone number
     */
    public String getTelephone() {
        return telephone;
    }

    /**
     * Returns the owner's pets in their stored order.
     *
     * @return the immutable pet list
     */
    public List<Pet> getPets() {
        return pets;
    }

    /**
     * Returns the owner's pets sorted by pet name.
     *
     * @return an immutable sorted pet list
     */
    @Transient
    public List<Pet> getPetsSorted() {
        List<Pet> sortedPets = new ArrayList<>(pets);
        sortedPets.sort(Comparator.comparing(Pet::name));
        return Collections.unmodifiableList(sortedPets);
    }

    /**
     * Returns a copy of this owner with a pet added to the aggregate.
     *
     * @param pet the pet to add
     * @return an owner copy containing the new pet
     */
    public Owner withPetAdded(Pet pet) {
        List<Pet> updatedPets = new ArrayList<>(pets);
        Pet ownerAwarePet = pet.withOwner(this);
        if (ownerAwarePet.isNew()) {
            updatedPets.add(ownerAwarePet);
        }
        return withPets(updatedPets);
    }

    /**
     * Returns a copy of this owner with a pet removed from the aggregate.
     *
     * @param pet the pet to remove
     * @return an owner copy without the pet
     */
    public Owner withoutPet(Pet pet) {
        List<Pet> updatedPets = new ArrayList<>(pets);
        updatedPets.remove(pet);
        return withPets(updatedPets);
    }

    /**
     * Finds a persisted pet by name.
     *
     * @param name the pet name to match, case-insensitively
     * @return the matching pet, or {@code null} when none is found
     */
    public Pet getPet(String name) {
        return getPet(name, false);
    }

    /**
     * Finds a pet by name.
     *
     * @param name the pet name to match, case-insensitively
     * @param ignoreNew whether new, unsaved pets should be included in the search
     * @return the matching pet, or {@code null} when none is found
     */
    public Pet getPet(String name, boolean ignoreNew) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String lowerCaseName = name.toLowerCase();
        for (Pet pet : pets) {
            if (!ignoreNew || !pet.isNew()) {
                if (pet.name().toLowerCase().equals(lowerCaseName)) {
                    return pet;
                }
            }
        }
        return null;
    }

    /**
     * Finds a pet by id.
     *
     * @param id the pet id
     * @return the matching pet, or {@code null} when none is found
     */
    public Pet getPet(Integer id) {
        for (Pet pet : pets) {
            if (pet.id().equals(id)) {
                return pet;
            }
        }
        return null;
    }

    /**
     * Compares owners by entity identity.
     *
     * @param other the object being compared
     * @return {@code true} when the other object represents the same owner
     */
    @Override
    public boolean equals(Object other) {
        return BaseEntity.entityEquals(this, other);
    }

    /**
     * Returns the entity-identity hash code.
     *
     * @return the hash code based on the owner id
     */
    @Override
    public int hashCode() {
        return BaseEntity.entityHashCode(this);
    }

    /**
     * Returns a diagnostic representation of the owner.
     *
     * @return a string containing the owner fields except pets
     */
    @Override
    public String toString() {
        return "Owner{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", telephone='" + telephone + '\'' +
                '}';
    }
}
