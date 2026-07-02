package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;

/**
 * Join table mapping for vets {@literal <->} specialities.
 *
 * The PetClinic schema models this as a pure join table without its own id.
 *
 * @param vetId the vet identifier
 * @param specialityId the speciality identifier
 */
@MappedEntity("VET_SPECIALTIES")
public record VetSpeciality(
        @MappedProperty("VET_ID")
        Integer vetId,

        @MappedProperty("SPECIALTY_ID")
        Integer specialityId
) {
}
