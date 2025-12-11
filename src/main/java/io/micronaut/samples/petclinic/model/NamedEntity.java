package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;

/**
 * Base entity class for entities that have a name property.
 * Extends BaseEntity with a name column.
 */
@MappedSuperclass
@Serdeable
public abstract class NamedEntity extends BaseEntity {

    @Column(name = "name")
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
