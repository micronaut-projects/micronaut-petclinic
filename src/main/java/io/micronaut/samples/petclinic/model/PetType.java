package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity representing a type of pet (e.g., dog, cat, bird).
 */
@Entity
@Table(name = "types")
@Serdeable
public class PetType extends NamedEntity {

    public PetType() {
    }

    public PetType(String name) {
        this.setName(name);
    }
}
