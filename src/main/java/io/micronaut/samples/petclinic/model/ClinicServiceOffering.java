package io.micronaut.samples.petclinic.model;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Index;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;

import java.math.BigDecimal;

import static io.micronaut.data.annotation.Relation.Kind.MANY_TO_ONE;

/**
 * A service that is offered by one clinic branch.
 *
 * @param id the database identifier
 * @param clinic the clinic branch that owns this offering
 * @param serviceCode the stable code used by the clinic's catalog
 * @param name the display name
 * @param description an optional description
 * @param price the price charged by this clinic
 * @param durationMinutes the expected duration in minutes
 */
@MappedEntity("CLINIC_SERVICE_OFFERINGS")
@Index(name = "UK_CLINIC_SERVICE_CODE", columns = {"CLINIC_ID", "SERVICE_CODE"}, unique = true)
public record ClinicServiceOffering(
        @Id
        @GeneratedValue
        Integer id,

        @Relation(MANY_TO_ONE)
        @MappedProperty("CLINIC_ID")
        Clinic clinic,

        @MappedProperty("SERVICE_CODE")
        String serviceCode,

        @MappedProperty("NAME")
        String name,

        @Nullable
        @MappedProperty("DESCRIPTION")
        String description,

        @MappedProperty("PRICE")
        BigDecimal price,

        @MappedProperty("DURATION_MINUTES")
        Integer durationMinutes
) {

    /**
     * Creates an empty instance for framework binding.
     */
    public ClinicServiceOffering() {
        this(null, null, null, null, null, null, null);
    }

    /**
     * Creates a new offering before its generated identifier is assigned.
     *
     * @param clinic the owning clinic
     * @param serviceCode the clinic-local service code
     * @param name the display name
     * @param description the optional description
     * @param price the clinic-specific price
     * @param durationMinutes the duration in minutes
     */
    public ClinicServiceOffering(Clinic clinic,
                                 String serviceCode,
                                 String name,
                                 String description,
                                 BigDecimal price,
                                 Integer durationMinutes) {
        this(null, clinic, serviceCode,
                name, description, price, durationMinutes);
    }
}
