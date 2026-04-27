package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.MappedEntity;

/**
 * Entity representing a type of pet (e.g., dog, cat, bird).
 */
@MappedEntity("types")
@Serdeable
public class PetType extends NamedEntity {

    public PetType() {
    }

    public PetType(String name) {
        this.setName(name);
    }
}
