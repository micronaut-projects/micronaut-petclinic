package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Data Transfer Object for Visit form submissions.
 *
 * @param date the submitted visit date
 * @param description the submitted visit description
 * @param durationMinutes the visit duration in minutes
 * @param followUpPeriodMonths the recommended follow-up period in months
 */
@Serdeable
public record VisitForm(
        @NotNull(message = "Visit date is required")
        @FutureOrPresent(message = "Visit date must be in the future")
        LocalDate date,

        @NotBlank(message = "Description is required")
        @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
        String description,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes,

        @NotNull(message = "Follow-up period is required")
        @Min(value = 1, message = "Follow-up period must be at least 1 month")
        Integer followUpPeriodMonths
) {

    /**
     * Creates an empty visit form for framework binding.
     */
    public VisitForm() {
        this(null, null, 30, 6);
    }
}
