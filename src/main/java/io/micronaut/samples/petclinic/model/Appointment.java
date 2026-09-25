package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import static io.micronaut.samples.petclinic.model.Appointment.Status.AVAILABLE;

/**
 * A small appointment resource used by the Oracle transaction-priority showcase.
 *
 * <p>The showcase uses two ordered appointments instead of managing dates,
 * calendars or visit durations. Availability is determined by status and choices
 * are ordered by display order. A production scheduler can extend this model;
 * the long-lived booking transactions remain demo-only.</p>
 *
 * @param id the database identifier
 * @param label the human-readable appointment label
 * @param displayOrder the order used to choose the current and fallback appointments
 * @param status the current booking state
 */
@MappedEntity("APPOINTMENTS")
@Serdeable
@Wither
public record Appointment(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("LABEL")
        @NotBlank
        String label,

        @MappedProperty("DISPLAY_ORDER")
        @NotNull
        Integer displayOrder,

        @MappedProperty("STATUS")
        @NotNull
        Status status
) implements BaseEntity, AppointmentWither {

    public enum Status {
        /** Appointment is available for a new booking. */
        AVAILABLE,
        /** A regular booking is holding the appointment in the demo transaction. */
        HELD_BY_REGULAR,
        /** A regular booking finished before an emergency could displace it. */
        BOOKED_FOR_REGULAR,
        /** A higher-priority emergency booking has claimed the appointment. */
        BOOKED_FOR_EMERGENCY
    }

    /**
     * Creates an empty appointment for framework binding.
     */
    public Appointment() {
        this(null, null, null, AVAILABLE);
    }

    /**
     * Creates a new available appointment resource.
     *
     * @param label appointment label
     * @param displayOrder order used by the showcase when finding the next resource
     */
    public Appointment(String label, Integer displayOrder) {
        this(null, label, displayOrder, AVAILABLE);
    }
}
