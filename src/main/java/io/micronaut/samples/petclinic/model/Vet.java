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
 * A vet can have multiple specialities.
 *
 * @param id the database identifier, or {@code null} for a new vet
 * @param firstName the vet's first name
 * @param lastName the vet's last name
 * @param specialities the transient specialities associated with the vet
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
        Set<Speciality> specialities
) implements Person, VetWither {

    /**
     * Creates a vet and normalizes a {@code null} speciality set to an empty immutable set.
     */
    public Vet {
        specialities = specialities != null ? Set.copyOf(specialities) : Set.of();
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
     * Creates a new vet without an id or specialities.
     *
     * @param firstName the vet's first name
     * @param lastName the vet's last name
     */
    public Vet(String firstName, String lastName) {
        this(null, firstName, lastName, Set.of());
    }

    /**
     * Returns the transient specialities associated with this vet.
     *
     * @return the immutable speciality set
     */
    @Override
    @Transient
    public Set<Speciality> specialities() {
        return specialities;
    }

    /**
     * Returns the vet specialities sorted by name.
     *
     * @return an immutable sorted speciality set
     */
    @Transient
    public Set<Speciality> getSpecialities() {
        List<Speciality> sortedSpecialities = new ArrayList<>(specialities());
        sortedSpecialities.sort(Comparator.comparing(Speciality::name));
        return Collections.unmodifiableSet(new LinkedHashSet<>(sortedSpecialities));
    }

    /**
     * Returns the number of specialities associated with this vet.
     *
     * @return the speciality count
     */
    @Transient
    public int getNrOfSpecialities() {
        return specialities.size();
    }

    /**
     * Returns a copy of this vet with a different id.
     *
     * @param id the replacement id
     * @return a vet copy with the supplied id
     */
    @Override
    public Vet withId(Integer id) {
        return new Vet(id, firstName, lastName, specialities);
    }

    /**
     * Returns a copy of this vet with a different first name.
     *
     * @param firstName the replacement first name
     * @return a vet copy with the supplied first name
     */
    @Override
    public Vet withFirstName(String firstName) {
        return new Vet(id, firstName, lastName, specialities);
    }

    /**
     * Returns a copy of this vet with a different last name.
     *
     * @param lastName the replacement last name
     * @return a vet copy with the supplied last name
     */
    @Override
    public Vet withLastName(String lastName) {
        return new Vet(id, firstName, lastName, specialities);
    }

    /**
     * Returns a copy of this vet with a different speciality set.
     *
     * @param specialities the replacement specialities
     * @return a vet copy with the supplied specialities
     */
    @Override
    public Vet withSpecialities(Set<Speciality> specialities) {
        return new Vet(id, firstName, lastName, specialities);
    }

    /**
     * Returns a copy of this vet with a speciality added.
     *
     * @param speciality the speciality to add
     * @return a vet copy containing the speciality
     */
    public Vet withSpecialityAdded(Speciality speciality) {
        Set<Speciality> updatedSpecialities = new TreeSet<>(Comparator.comparing(Speciality::name));
        updatedSpecialities.addAll(specialities);
        updatedSpecialities.add(speciality);
        return withSpecialities(updatedSpecialities);
    }

    /**
     * Returns a copy of this vet with a speciality removed.
     *
     * @param speciality the speciality to remove
     * @return a vet copy without the speciality
     */
    public Vet withoutSpeciality(Speciality speciality) {
        Set<Speciality> updatedSpecialities = new TreeSet<>(Comparator.comparing(Speciality::name));
        updatedSpecialities.addAll(specialities);
        updatedSpecialities.remove(speciality);
        return withSpecialities(updatedSpecialities);
    }

    /**
     * Returns a comma-separated list of speciality names for display.
     *
     * @return the speciality names, or {@code none} when the vet has no specialities
     */
    @Transient
    public String getSpecialitiesAsString() {
        if (specialities.isEmpty()) {
            return "none";
        }
        List<String> names = new ArrayList<>();
        for (Speciality speciality : getSpecialities()) {
            names.add(speciality.name());
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
                ", specialities=" + getSpecialitiesAsString() +
                '}';
    }
}
