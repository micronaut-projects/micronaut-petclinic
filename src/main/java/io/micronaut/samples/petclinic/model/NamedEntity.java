package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Base entity class for entities that have a name property.
 * Extends BaseEntity with a name column.
 */
@MappedEntity
@Serdeable
public abstract class NamedEntity extends BaseEntity {

    @MappedProperty("name")
    @NotBlank
    private String name;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
