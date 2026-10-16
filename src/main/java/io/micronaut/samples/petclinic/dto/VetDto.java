package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;

import java.util.Set;

/**
 * JSON representation of a veterinarian and its speciality references.
 *
 * @param id veterinarian identifier, or {@code null} when creating
 * @param firstName veterinarian first name
 * @param lastName veterinarian last name
 * @param specialities speciality references; omitted selections become an empty set
 * @param etag the loaded ETag, or {@code null} for a new veterinarian
 */
@Serdeable
public record VetDto(
        @Nullable Integer id,
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NonNull Set<SpecialityDto> specialities,
        @Nullable String etag
) {
    /**
     * Copies the speciality set so callers cannot mutate the DTO.
     *
     * @param id veterinarian identifier, or {@code null} when creating
     * @param firstName veterinarian first name
     * @param lastName veterinarian last name
     * @param specialities speciality references; omitted selections become an empty set
     * @param etag the loaded ETag, or {@code null} for a new veterinarian
     */
    public VetDto {
        specialities = specialities == null ? Set.of() : Set.copyOf(specialities);
    }
}
