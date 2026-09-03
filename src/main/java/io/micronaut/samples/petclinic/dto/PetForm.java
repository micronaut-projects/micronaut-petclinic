package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data Transfer Object for Pet form submissions.
 *
 * @param id the pet id for update submissions, or {@code null} for new pets
 * @param name the submitted pet name
 * @param birthDate the submitted birth date
 * @param typeId the submitted pet type id
 */
@Serdeable
public record PetForm(
        Integer id,

        @NotBlank(message = "Pet name is required")
        @Size(min = 1, max = 30, message = "Pet name must be between 1 and 30 characters")
        String name,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate,

        @NotNull(message = "Pet type is required")
        Integer typeId
) {

    /**
     * Creates an empty pet form for framework binding.
     */
    public PetForm() {
        this(null, null, null, null);
    }
}
