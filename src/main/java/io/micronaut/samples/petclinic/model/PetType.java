package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing a type of pet (e.g., dog, cat, bird).
 *
 * @param id the database identifier, or {@code null} for a new pet type
 * @param name the display name of the pet type
 */
@MappedEntity("TYPES")
@Serdeable
@Wither
public record PetType(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("NAME")
        @NotBlank
        String name
) implements NamedEntity, PetTypeWither {

    /**
     * Creates an empty pet type for framework binding.
     */
    public PetType() {
        this(null, null);
    }

    /**
     * Creates a new pet type without an id.
     *
     * @param name the display name of the pet type
     */
    public PetType(String name) {
        this(null, name);
    }

    /**
     * Compares pet types by entity identity.
     *
     * @param other the object being compared
     * @return {@code true} when the other object represents the same pet type
     */
    @Override
    public boolean equals(Object other) {
        return BaseEntity.entityEquals(this, other);
    }

    /**
     * Returns the entity-identity hash code.
     *
     * @return the hash code based on the pet type id
     */
    @Override
    public int hashCode() {
        return BaseEntity.entityHashCode(this);
    }

    /**
     * Returns the pet type display name.
     *
     * @return the pet type name
     */
    @Override
    public String toString() {
        return name;
    }
}
