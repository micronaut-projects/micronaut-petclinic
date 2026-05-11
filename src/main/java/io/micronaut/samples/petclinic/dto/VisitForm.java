package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data Transfer Object for Visit form submissions.
 *
 * @param date the submitted visit date
 * @param description the submitted visit description
 */
@Introspected
@Serdeable
public record VisitForm(
        @NotNull(message = "Visit date is required")
        LocalDate date,

        @NotBlank(message = "Description is required")
        @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
        String description
) {

    /**
     * Creates an empty visit form for framework binding.
     */
    public VisitForm() {
        this(null, null);
    }
}
