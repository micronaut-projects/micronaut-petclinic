package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Request model for the veterinarian create and edit pages.
 *
 * @param firstName veterinarian first name
 * @param lastName veterinarian last name
 * @param specialityIds selected speciality identifiers
 */
@Introspected
@Serdeable
public record VetForm(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        Set<Integer> specialityIds
) {
    /**
     * Copies selected speciality IDs, treating an omitted selection as empty.
     *
     * @param firstName veterinarian first name
     * @param lastName veterinarian last name
     * @param specialityIds selected speciality identifiers; omitted selections become an empty set
     */
    public VetForm {
        specialityIds = specialityIds == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(specialityIds));
    }

    /**
     * Creates an empty model for the veterinarian creation page.
     */
    public VetForm() {
        this(null, null, Set.of());
    }

    /**
     * Creates a veterinarian entity from this form without copying its ETag.
     *
     * @param specialities resolved speciality entities
     * @return the veterinarian with no ETag
     */
    public Vet toVet(Set<Speciality> specialities) {
        return new Vet(null, firstName, lastName, specialities, null);
    }
}
