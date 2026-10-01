package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.samples.petclinic.repository.OracleAppointmentLockRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.transaction.exceptions.OracleTransactionPriorityException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.micronaut.samples.petclinic.model.Appointment.Status.BOOKED_FOR_EMERGENCY;
import static io.micronaut.samples.petclinic.model.Appointment.Status.BOOKED_FOR_REGULAR;
import static io.micronaut.samples.petclinic.support.OracleTransactionPriorityTestSupport.awaitRegularLock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(transactional = false)
@Requires(env = "oracle")
@Property(name = "petclinic.transaction-priority.reservation-seconds", value = "5")
class OracleTransactionPriorityIntegrationTest {

    @Inject OracleTransactionPriorityService service;
    @Inject AppointmentRepository appointments;
    @Inject OracleAppointmentLockRepository appointmentLocks;

    private Appointment originalAppointment;

    @BeforeEach
    void useExistingAppointment() {
        originalAppointment = appointments.findAvailableAppointments().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "An available sample appointment is required for the priority test."));
    }

    @AfterEach
    void restoreAppointment() {
        if (originalAppointment != null) appointments.save(originalAppointment);
    }

    @Test
    void regularBookingCommitsWithoutAnEmergency() {
        assertThat(service.getReservationSeconds()).isEqualTo(5);
        service.bookRegular(originalAppointment.id());
        assertThat(appointments.findById(originalAppointment.id()).orElseThrow().status()).isEqualTo(BOOKED_FOR_REGULAR);
    }

    @Test
    void emergencyCommitsAndOracleRollsBackRegularBooking() throws Exception {
        assertThat(service.getReservationSeconds()).isEqualTo(5); // Exceeds Oracle's 3-second HIGH wait target.
        // Closing the executor waits for LOW before @AfterEach restores the appointment.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var regular = executor.submit(() -> service.bookRegular(originalAppointment.id()));
            awaitRegularLock(appointmentLocks, originalAppointment.id());
            service.bookEmergency(originalAppointment.id());
            var failure = assertThrows(ExecutionException.class, () -> regular.get(30, TimeUnit.SECONDS));
            assertThat(failure.getCause()).isInstanceOf(OracleTransactionPriorityException.class);
            assertThat(appointments.findById(originalAppointment.id()).orElseThrow().status()).isEqualTo(BOOKED_FOR_EMERGENCY);
        }
    }
}
