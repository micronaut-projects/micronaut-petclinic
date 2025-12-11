package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity representing a veterinarian specialty (e.g., surgery, dentistry).
 */
@Entity
@Table(name = "specialties")
@Serdeable
public class Specialty extends NamedEntity {

    public Specialty() {
    }

    public Specialty(String name) {
        this.setName(name);
    }
}
