package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

/**
 * Join table mapping for vets <-> specialties.
 *
 * The Petclinic schema models this as a pure join table without its own id.
 */
@MappedEntity("vet_specialties")
public class VetSpecialty {

    @MappedProperty("vet_id")
    private Integer vetId;

    @MappedProperty("specialty_id")
    private Integer specialtyId;

    public Integer getVetId() {
        return vetId;
    }

    public void setVetId(Integer vetId) {
        this.vetId = vetId;
    }

    public Integer getSpecialtyId() {
        return specialtyId;
    }

    public void setSpecialtyId(Integer specialtyId) {
        this.specialtyId = specialtyId;
    }
}
