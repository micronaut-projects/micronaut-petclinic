package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Transient;
import io.micronaut.core.annotation.Creator;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Entity representing a veterinarian.
 * A vet can have multiple specialties.
 *
 * @param id the database identifier, or {@code null} for a new vet
 * @param firstName the vet's first name
 * @param lastName the vet's last name
 * @param specialties the transient specialties associated with the vet
 */
@MappedEntity("VETS")
@Serdeable
@Wither
public record Vet(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("FIRST_NAME")
        @NotBlank
        String firstName,

        @MappedProperty("LAST_NAME")
        @NotBlank
        String lastName,

        @Transient
        Set<Specialty> specialties
) implements Person, VetWither {

    /**
     * Creates a vet and normalizes a {@code null} specialty set to an empty immutable set.
     */
    public Vet {
        specialties = specialties != null ? Set.copyOf(specialties) : Set.of();
    }

    /**
     * Creates an empty vet for framework binding.
     */
    public Vet() {
        this(null, null, null, Set.of());
    }

    /**
     * Creates a vet from persisted columns only.
     *
     * @param id the database identifier
     * @param firstName the vet's first name
     * @param lastName the vet's last name
     */
    @Creator
    public Vet(Integer id, String firstName, String lastName) {
        this(id, firstName, lastName, Set.of());
    }

    /**
     * Creates a new vet without an id or specialties.
     *
     * @param firstName the vet's first name
     * @param lastName the vet's last name
     */
    public Vet(String firstName, String lastName) {
        this(null, firstName, lastName, Set.of());
    }

    /**
     * Returns the transient specialties associated with this vet.
     *
     * @return the immutable specialty set
     */
    @Override
    @Transient
    public Set<Specialty> specialties() {
        return specialties;
    }

    /**
     * Returns the vet specialties sorted by name.
     *
     * @return an immutable sorted specialty set
     */
    @Transient
    public Set<Specialty> getSpecialties() {
        List<Specialty> sortedSpecialties = new ArrayList<>(specialties());
        sortedSpecialties.sort(Comparator.comparing(Specialty::name));
        return Collections.unmodifiableSet(new LinkedHashSet<>(sortedSpecialties));
    }

    /**
     * Returns the number of specialties associated with this vet.
     *
     * @return the specialty count
     */
    @Transient
    public int getNrOfSpecialties() {
        return specialties.size();
    }

    /**
     * Returns a copy of this vet with a different id.
     *
     * @param id the replacement id
     * @return a vet copy with the supplied id
     */
    @Override
    public Vet withId(Integer id) {
        return new Vet(id, firstName, lastName, specialties);
    }

    /**
     * Returns a copy of this vet with a different first name.
     *
     * @param firstName the replacement first name
     * @return a vet copy with the supplied first name
     */
    @Override
    public Vet withFirstName(String firstName) {
        return new Vet(id, firstName, lastName, specialties);
    }

    /**
     * Returns a copy of this vet with a different last name.
     *
     * @param lastName the replacement last name
     * @return a vet copy with the supplied last name
     */
    @Override
    public Vet withLastName(String lastName) {
        return new Vet(id, firstName, lastName, specialties);
    }

    /**
     * Returns a copy of this vet with a different specialty set.
     *
     * @param specialties the replacement specialties
     * @return a vet copy with the supplied specialties
     */
    @Override
    public Vet withSpecialties(Set<Specialty> specialties) {
        return new Vet(id, firstName, lastName, specialties);
    }

    /**
     * Returns a copy of this vet with a specialty added.
     *
     * @param specialty the specialty to add
     * @return a vet copy containing the specialty
     */
    public Vet withSpecialtyAdded(Specialty specialty) {
        Set<Specialty> updatedSpecialties = new TreeSet<>(Comparator.comparing(Specialty::name));
        updatedSpecialties.addAll(specialties);
        updatedSpecialties.add(specialty);
        return withSpecialties(updatedSpecialties);
    }

    /**
     * Returns a copy of this vet with a specialty removed.
     *
     * @param specialty the specialty to remove
     * @return a vet copy without the specialty
     */
    public Vet withoutSpecialty(Specialty specialty) {
        Set<Specialty> updatedSpecialties = new TreeSet<>(Comparator.comparing(Specialty::name));
        updatedSpecialties.addAll(specialties);
        updatedSpecialties.remove(specialty);
        return withSpecialties(updatedSpecialties);
    }

    /**
     * Returns a comma-separated list of specialty names for display.
     *
     * @return the specialty names, or {@code none} when the vet has no specialties
     */
    @Transient
    public String getSpecialtiesAsString() {
        if (specialties.isEmpty()) {
            return "none";
        }
        List<String> names = new ArrayList<>();
        for (Specialty specialty : getSpecialties()) {
            names.add(specialty.name());
        }
        return String.join(", ", names);
    }

    /**
     * Compares vets by entity identity.
     *
     * @param other the object being compared
     * @return {@code true} when the other object represents the same vet
     */
    @Override
    public boolean equals(Object other) {
        return BaseEntity.entityEquals(this, other);
    }

    /**
     * Returns the entity-identity hash code.
     *
     * @return the hash code based on the vet id
     */
    @Override
    public int hashCode() {
        return BaseEntity.entityHashCode(this);
    }

    /**
     * Returns a diagnostic representation of the vet.
     *
     * @return a string containing the vet fields
     */
    @Override
    public String toString() {
        return "Vet{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", specialties=" + getSpecialtiesAsString() +
                '}';
    }
}
