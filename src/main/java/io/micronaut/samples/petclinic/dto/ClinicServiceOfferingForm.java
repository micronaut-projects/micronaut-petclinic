package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.samples.petclinic.model.ClinicServiceOffering;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Form used to create or update one clinic's service offering.
 *
 * <p>The clinic id is supplied by the URL rather than the form, which prevents
 * a caller from moving an offering to another clinic while editing it.</p>
 *
 * @param serviceCode the clinic-local stable code
 * @param name the display name
 * @param description the optional description
 * @param price the clinic-specific price
 * @param durationMinutes the expected duration in minutes
 */
@Introspected
@Serdeable
public record ClinicServiceOfferingForm(
        @NotBlank(message = "Service code is required")
        @Size(max = 20, message = "Service code must be at most 40 characters")
        String serviceCode,

        @NotBlank(message = "Service name is required")
        @Size(max = 100, message = "Service name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Price must have at most two decimal places")
        BigDecimal price,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationMinutes
) {

    /**
     * Creates a form with useful defaults for the first new offering.
     */
    public ClinicServiceOfferingForm() {
        this(null, null, null, BigDecimal.ZERO, 30);
    }

    /**
     * Creates a form from an existing offering.
     *
     * @param offering the persisted offering
     * @return a form containing its editable fields
     */
    public static ClinicServiceOfferingForm from(ClinicServiceOffering offering) {
        return new ClinicServiceOfferingForm(
                offering.serviceCode(),
                offering.name(),
                offering.description(),
                offering.price(),
                offering.durationMinutes());
    }
}
