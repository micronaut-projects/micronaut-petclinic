package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Speciality name and identifier used by the JSON API and forms.
 *
 * @param id speciality identifier, or {@code null} when creating
 * @param name speciality name; required when creating or renaming
 */
@Serdeable
public record SpecialityDto(
        @Nullable Integer id,
        @NotBlank @Size(max = 255) String name
) {
}
