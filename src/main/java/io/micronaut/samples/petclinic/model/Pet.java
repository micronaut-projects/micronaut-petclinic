package io.micronaut.samples.petclinic.model;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.annotation.Transient;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Builder;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static io.micronaut.data.annotation.Relation.Kind.MANY_TO_ONE;
import static io.micronaut.data.annotation.Relation.Kind.ONE_TO_MANY;

/**
 * Entity representing a pet.
 * A pet belongs to an owner and has a type.
 *
 * @param id the database identifier, or {@code null} for a new pet
 * @param name the pet's name
 * @param birthDate the pet's birth date
 * @param type the pet type
 * @param owner the owning owner
 * @param visits the visits recorded for the pet
 */
@MappedEntity("PETS")
@Serdeable
@Builder
@Wither
public record Pet(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("NAME")
        @NotBlank
        String name,

        @MappedProperty("BIRTH_DATE")
        @NotNull
        LocalDate birthDate,

        @Relation(MANY_TO_ONE)
        @MappedProperty("TYPE_ID")
        @NotNull
        PetType type,

        @Relation(MANY_TO_ONE)
        @MappedProperty("OWNER_ID")
        Owner owner,

        @Relation(value = ONE_TO_MANY, mappedBy = "pet")
        List<Visit> visits
) implements NamedEntity, PetWither {

    /**
     * Creates a pet and normalizes a {@code null} visit list to an empty immutable list.
     *
     * @param id the database identifier, or {@code null} for a new pet
     * @param name the pet's name
     * @param birthDate the pet's birth date
     * @param type the pet type
     * @param owner the owning owner
     * @param visits the visits recorded for the pet
     */
    public Pet(Integer id,
               String name,
               LocalDate birthDate,
               PetType type,
               @Nullable Owner owner,
               List<Visit> visits) {
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
        this.type = type;
        this.owner = owner;
        this.visits = visits != null ? List.copyOf(visits) : List.of();
    }

    /**
     * Creates an empty pet for framework binding.
     */
    public Pet() {
        this(null, null, null, null, null, List.of());
    }

    /**
     * Creates a new pet without an id or visits.
     *
     * @param name the pet's name
     * @param birthDate the pet's birth date
     * @param type the pet type
     * @param owner the owning owner
     */
    public Pet(String name, LocalDate birthDate, PetType type, Owner owner) {
        this(null, name, birthDate, type, owner, List.of());
    }

    /**
     * Returns the pet's birth date.
     *
     * @return the birth date
     */
    public LocalDate getBirthDate() {
        return birthDate;
    }

    /**
     * Returns the pet type.
     *
     * @return the pet type
     */
    public PetType getType() {
        return type;
    }

    /**
     * Returns the identifier of the pet type.
     *
     * @return the type id, or {@code null} when no type is assigned
     */
    @Transient
    public Integer getTypeId() {
        return type != null ? type.id() : null;
    }

    /**
     * Returns the owner of this pet.
     *
     * @return the owning owner
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Returns the identifier of the owning owner.
     *
     * @return the owner id, or {@code null} when no owner is assigned
     */
    @Transient
    public Integer getOwnerId() {
        return owner != null ? owner.id() : null;
    }

    /**
     * Returns visits sorted by visit date.
     *
     * @return an immutable sorted visit list
     */
    public List<Visit> getVisits() {
        List<Visit> sortedVisits = new ArrayList<>(visits);
        sortedVisits.sort(Comparator.comparing(Visit::date));
        return Collections.unmodifiableList(sortedVisits);
    }

    /**
     * Returns a copy of this pet with a visit added to the aggregate.
     *
     * @param visit the visit to add
     * @return a pet copy containing the new visit
     */
    public Pet withVisitAdded(Visit visit) {
        List<Visit> updatedVisits = new ArrayList<>(visits);
        updatedVisits.add(visit.withPet(this));
        return withVisits(updatedVisits);
    }

    /**
     * Compares pets by entity identity.
     *
     * @param other the object being compared
     * @return {@code true} when the other object represents the same pet
     */
    @Override
    public boolean equals(Object other) {
        return BaseEntity.entityEquals(this, other);
    }

    /**
     * Returns the entity-identity hash code.
     *
     * @return the hash code based on the pet id
     */
    @Override
    public int hashCode() {
        return BaseEntity.entityHashCode(this);
    }

    /**
     * Returns a diagnostic representation of the pet.
     *
     * @return a string containing the pet fields except owner and visits
     */
    @Override
    public String toString() {
        return "Pet{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", birthDate=" + birthDate +
                ", type=" + (type != null ? type.name() : null) +
                '}';
    }
}
