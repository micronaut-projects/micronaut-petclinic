package io.micronaut.samples.petclinic.model;

import io.micronaut.core.annotation.Creator;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.annotation.Transient;
import io.micronaut.data.annotation.sql.ETagValue;
import io.micronaut.data.annotation.sql.ETaggable;
import io.micronaut.data.annotation.sql.GeneratedETag;
import io.micronaut.data.annotation.sql.JoinColumn;
import io.micronaut.data.annotation.sql.JoinTable;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Entity representing a veterinarian.
 * A vet can have multiple specialities.
 *
 * @param id the database identifier, or {@code null} for a new vet
 * @param firstName the vet's first name
 * @param lastName the vet's last name
 * @param specialities the specialities associated with the vet
 * @param etag the generated optimistic-locking value
 */
@MappedEntity("VETS")
@Serdeable
@Wither
@ETaggable
public record Vet(
        @Id
        @GeneratedValue
        @ETagValue
        Integer id,

        @ETagValue
        @NotBlank
        String firstName,

        @ETagValue
        @NotBlank
        String lastName,

        @Relation(value = Relation.Kind.MANY_TO_MANY, cascade = {
                Relation.Cascade.PERSIST,
                Relation.Cascade.UPDATE
        })
        @JoinTable(
                name = "VET_SPECIALTIES",
                joinColumns = @JoinColumn(name = "VET_ID", referencedColumnName = "id"),
                inverseJoinColumns = @JoinColumn(name = "SPECIALTY_ID", referencedColumnName = "id")
        )
        @NonNull
        Set<Speciality> specialities,

        @Nullable
        @GeneratedETag(function = "SYS_ROW_ETAG")
        String etag
) implements Person, VetWither {

    /**
     * Creates an immutable veterinarian with an independent speciality set.
     *
     * @param id veterinarian identifier, or {@code null} when creating
     * @param firstName veterinarian first name
     * @param lastName veterinarian last name
     * @param specialities speciality references; omitted selections become an empty set
     * @param etag the loaded ETag, or {@code null} for a new veterinarian
     */
    @Creator
    public Vet {
        specialities = specialities == null ? Set.of() : Set.copyOf(specialities);
    }

    /**
     * Creates an empty vet for framework binding.
     */
    public Vet() {
        this(null, null, null, Set.of(), null);
    }

    /**
     * Creates a vet from persisted columns only.
     *
     * @param id the database identifier
     * @param firstName the vet's first name
     * @param lastName the vet's last name
     */
    public Vet(Integer id, String firstName, String lastName) {
        this(id, firstName, lastName, Set.of(), null);
    }

    /**
     * Creates a new vet without an id or specialities.
     *
     * @param firstName the vet's first name
     * @param lastName the vet's last name
     */
    public Vet(String firstName, String lastName) {
        this(null, firstName, lastName, Set.of(), null);
    }

    /**
     * Returns the vet specialities sorted by name, with unresolved names first.
     *
     * @return an immutable sorted speciality set
     */
    public Set<Speciality> specialities() {
        List<Speciality> sortedSpecialities = new ArrayList<>(this.specialities);
        sortedSpecialities.sort(Comparator.comparing(Speciality::name, Comparator.nullsFirst(Comparator.naturalOrder())));
        return Collections.unmodifiableSet(new LinkedHashSet<>(sortedSpecialities));
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
        for (Speciality speciality : specialities()) {
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
