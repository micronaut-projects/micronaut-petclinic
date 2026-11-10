package io.micronaut.samples.petclinic.dto;

import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.serde.annotation.Serdeable;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Result of a hold or completion after the transaction boundary has returned.
 *
 * <p>For a hold, {@code databaseStatus} and {@code appointments} show committed state;
 * they do not expose the uncommitted hold.</p>
 *
 * @param appointmentId identifier of the appointment
 * @param outcome operation completed by the request
 * @param transactionId opaque handle returned for a hold, or {@code null} after completion
 * @param holdSeconds time in seconds before Oracle expires a suspended hold
 * @param databaseStatus committed status of the requested appointment
 * @param appointments committed appointment states for the sessionless showcase
 */
@Serdeable
public record SessionlessBookingResult(
        Integer appointmentId,
        Outcome outcome,
        @Nullable String transactionId,
        int holdSeconds,
        Appointment.Status databaseStatus,
        List<Appointment> appointments
) {
    @Serdeable
    public enum Outcome {
        /** The transaction was suspended and its handle was returned. */
        HELD,
        /** The suspended transaction resumed and committed the booking. */
        CONFIRMED,
        /** The suspended transaction resumed and rolled back the hold. */
        CANCELLED
    }
}
