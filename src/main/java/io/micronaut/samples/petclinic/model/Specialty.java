package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.MappedEntity;

/**
 * Entity representing a veterinarian specialty (e.g., surgery, dentistry).
 */
@MappedEntity("specialties")
@Serdeable
public class Specialty extends NamedEntity {

    public Specialty() {
    }

    public Specialty(String name) {
        this.setName(name);
    }
}
