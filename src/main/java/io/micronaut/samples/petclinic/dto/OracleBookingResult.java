package io.micronaut.samples.petclinic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

/** Final result of one booking request, after its transaction has settled. */
@Serdeable
@JsonInclude(JsonInclude.Include.ALWAYS)
public record OracleBookingResult(
        Integer appointmentId,
        Outcome outcome,
        Appointment.Status databaseStatus,
        List<Appointment> availableAppointments
) {
    @Serdeable
    public enum Outcome {
        COMMITTED, PRIORITY_ROLLED_BACK, TAKEN, TIMED_OUT, FAILED
    }
}
