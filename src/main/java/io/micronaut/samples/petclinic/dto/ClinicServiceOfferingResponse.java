package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.samples.petclinic.model.ClinicServiceOffering;

import java.math.BigDecimal;

/**
 * JSON representation returned after a clinic service offering is upserted.
 *
 * <p>The response exposes the clinic identifier without serializing the full
 * {@code Clinic} entity, whose geospatial properties are persistence-focused.</p>
 *
 * @param id the persisted offering identifier
 * @param clinicId the owning clinic identifier
 * @param serviceCode the clinic-local service code
 * @param name the display name
 * @param description the optional description
 * @param price the clinic-specific price
 * @param durationMinutes the duration in minutes
 */
@Serdeable
public record ClinicServiceOfferingResponse(
        Integer id,
        Integer clinicId,
        String serviceCode,
        String name,
        String description,
        BigDecimal price,
        Integer durationMinutes
) {

    /**
     * Creates an API response from the persisted entity.
     *
     * @param offering the persisted offering
     * @return the JSON-safe response
     */
    public static ClinicServiceOfferingResponse from(ClinicServiceOffering offering) {
        return new ClinicServiceOfferingResponse(
                offering.id(),
                offering.clinic() == null ? null : offering.clinic().id(),
                offering.serviceCode(),
                offering.name(),
                offering.description(),
                offering.price(),
                offering.durationMinutes());
    }
}
