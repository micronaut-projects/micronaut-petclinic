package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import static io.micronaut.data.annotation.Relation.Kind.ONE_TO_MANY;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import java.util.*;

/**
 * Entity representing a pet owner.
 * An owner can have multiple pets.
 */
@MappedEntity("owners")
@Serdeable
public class Owner extends Person {

    @MappedProperty("address")
    @NotBlank
    private String address;

    @MappedProperty("city")
    @NotBlank
    private String city;

    @MappedProperty("telephone")
    @NotBlank
    @Digits(fraction = 0, integer = 10)
    private String telephone;

    @Relation(value = ONE_TO_MANY, mappedBy = "owner", cascade = Relation.Cascade.ALL)
    private List<Pet> pets = new ArrayList<>();

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTelephone() {
        return this.telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    /**
     * Get all pets belonging to this owner.
     *
     * Note: This returns the live association list so Micronaut Data JDBC can
     * populate it when using @Join fetches.
     */
    public List<Pet> getPets() {
        return this.pets;
    }

    public void setPets(List<Pet> pets) {
        this.pets = pets != null ? pets : new ArrayList<>();
    }

    @io.micronaut.data.annotation.Transient
    public List<Pet> getPetsSorted() {
        List<Pet> sortedPets = new ArrayList<>(this.pets);
        sortedPets.sort(Comparator.comparing(Pet::getName));
        return Collections.unmodifiableList(sortedPets);
    }

    /**
     * Add a pet to this owner.
     * @param pet the pet to add
     */
    public void addPet(Pet pet) {
        if (pet.isNew()) {
            this.pets.add(pet);
        }
        pet.setOwner(this);
    }

    /**
     * Remove a pet from this owner.
     * @param pet the pet to remove
     */
    public void removePet(Pet pet) {
        this.pets.remove(pet);
        pet.setOwner(null);
    }

    /**
     * Get a pet by name.
     * @param name the pet name to search for
     * @return the pet, or null if not found
     */
    public Pet getPet(String name) {
        return getPet(name, false);
    }

    /**
     * Get a pet by name, optionally ignoring new pets.
     * @param name the pet name to search for
     * @param ignoreNew whether to ignore pets not yet persisted
     * @return the pet, or null if not found
     */
    public Pet getPet(String name, boolean ignoreNew) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String lowerCaseName = name.toLowerCase();
        for (Pet pet : this.pets) {
            if (!ignoreNew || !pet.isNew()) {
                if (pet.getName().toLowerCase().equals(lowerCaseName)) {
                    return pet;
                }
            }
        }
        return null;
    }

    /**
     * Get a pet by ID.
     * @param id the pet ID
     * @return the pet, or null if not found
     */
    public Pet getPet(Integer id) {
        for (Pet pet : this.pets) {
            if (pet.getId().equals(id)) {
                return pet;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Owner{" +
                "id=" + getId() +
                ", firstName='" + getFirstName() + '\'' +
                ", lastName='" + getLastName() + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", telephone='" + telephone + '\'' +
                '}';
    }
}
