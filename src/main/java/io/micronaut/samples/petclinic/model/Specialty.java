package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing a veterinarian specialty (e.g., surgery, dentistry).
 *
 * @param id the database identifier, or {@code null} for a new specialty
 * @param name the display name of the specialty
 */
@MappedEntity("SPECIALTIES")
@Serdeable
@Wither
public record Specialty(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("NAME")
        @NotBlank
        String name
) implements NamedEntity, SpecialtyWither {

    /**
     * Creates an empty specialty for framework binding.
     */
    public Specialty() {
        this(null, null);
    }

    /**
     * Creates a new specialty without an id.
     *
     * @param name the display name of the specialty
     */
    public Specialty(String name) {
        this(null, name);
    }

    /**
     * Compares specialties by entity identity.
     *
     * @param other the object being compared
     * @return {@code true} when the other object represents the same specialty
     */
    @Override
    public boolean equals(Object other) {
        return BaseEntity.entityEquals(this, other);
    }

    /**
     * Returns the entity-identity hash code.
     *
     * @return the hash code based on the specialty id
     */
    @Override
    public int hashCode() {
        return BaseEntity.entityHashCode(this);
    }

    /**
     * Returns the specialty display name.
     *
     * @return the specialty name
     */
    @Override
    public String toString() {
        return name;
    }
}
