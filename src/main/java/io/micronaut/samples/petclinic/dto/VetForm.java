package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for veterinarian form submissions.
 *
 * @param id the vet id for update submissions, or {@code null} for new vets
 * @param firstName the submitted first name
 * @param lastName the submitted last name
 */
@Serdeable
public record VetForm(
        Integer id,

        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 30, message = "First name must be between 1 and 30 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 30, message = "Last name must be between 1 and 30 characters")
        String lastName
) {

    /**
     * Creates an empty vet form for framework binding.
     */
    public VetForm() {
        this(null, null, null);
    }
}
