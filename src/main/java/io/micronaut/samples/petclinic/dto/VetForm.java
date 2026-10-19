package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

/**
 * Request model for the veterinarian create and edit pages.
 *
 * @param id veterinarian identifier, or {@code null} when creating
 * @param firstName veterinarian first name
 * @param lastName veterinarian last name
 * @param specialityIds selected speciality identifiers
 */
@Introspected
@Serdeable
public record VetForm(
        @Nullable Integer id,
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        Set<Integer> specialityIds
) {
    /**
     * Copies selected speciality IDs, treating an omitted selection as empty.
     *
     * @param id veterinarian identifier, or {@code null} when creating
     * @param firstName veterinarian first name
     * @param lastName veterinarian last name
     * @param specialityIds selected speciality identifiers; omitted selections become an empty set
     * @param etag the loaded ETag, or {@code null} for a new veterinarian
     */
    public VetForm {
        specialityIds = specialityIds == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(specialityIds));
    }

    /**
     * Creates an empty model for the veterinarian creation page.
     */
    public VetForm() {
        this(null, null, null, Set.of());
    }

    /**
     * Creates a veterinarian entity from this form without copying its ETag.
     *
     * @param vetId persisted identifier, or {@code null} when creating
     * @param specialities resolved speciality entities
     * @return the veterinarian with no ETag
     */
    public Vet toVet(Integer vetId, Set<Speciality> specialities) {
        return new Vet(vetId, firstName, lastName, specialities, null);
    }
}
