package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Owner form submissions.
 *
 * @param id the owner id for update submissions, or {@code null} for new owners
 * @param firstName the submitted first name
 * @param lastName the submitted last name
 * @param address the submitted street address
 * @param city the submitted city
 * @param telephone the submitted 10-digit telephone number
 */
@Serdeable
public record OwnerForm(
        Integer id,

        @NotBlank(message = "First name is required")
        @Size(min = 1, max = 30, message = "First name must be between 1 and 30 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 1, max = 30, message = "Last name must be between 1 and 30 characters")
        String lastName,

        @NotBlank(message = "Address is required")
        @Size(min = 1, max = 255, message = "Address must be between 1 and 255 characters")
        String address,

        @NotBlank(message = "City is required")
        @Size(min = 1, max = 80, message = "City must be between 1 and 80 characters")
        String city,

        @NotBlank(message = "Telephone is required")
        @Pattern(regexp = "\\d{10}", message = "Telephone must be a 10-digit number")
        String telephone
) {

    /**
     * Creates an empty owner form for framework binding.
     */
    public OwnerForm() {
        this(null, null, null, null, null, null);
    }
}
