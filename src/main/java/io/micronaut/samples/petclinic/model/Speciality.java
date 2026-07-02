package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing a veterinarian speciality (e.g., surgery, dentistry).
 *
 * @param id the database identifier, or {@code null} for a new speciality
 * @param name the display name of the speciality
 */
@MappedEntity("SPECIALTIES")
@Serdeable
@Wither
public record Speciality(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("NAME")
        @NotBlank
        String name
) implements NamedEntity, SpecialityWither {

    /**
     * Creates an empty speciality for framework binding.
     */
    public Speciality() {
        this(null, null);
    }

    /**
     * Creates a new speciality without an id.
     *
     * @param name the display name of the speciality
     */
    public Speciality(String name) {
        this(null, name);
    }

    /**
     * Compares specialities by entity identity.
     *
     * @param other the object being compared
     * @return {@code true} when the other object represents the same speciality
     */
    @Override
    public boolean equals(Object other) {
        return BaseEntity.entityEquals(this, other);
    }

    /**
     * Returns the entity-identity hash code.
     *
     * @return the hash code based on the speciality id
     */
    @Override
    public int hashCode() {
        return BaseEntity.entityHashCode(this);
    }

    /**
     * Returns the speciality display name.
     *
     * @return the speciality name
     */
    @Override
    public String toString() {
        return name;
    }
}
