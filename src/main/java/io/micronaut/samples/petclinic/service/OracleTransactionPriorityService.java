package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.transaction.annotation.OracleTransactional;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.time.Duration;

import static io.micronaut.samples.petclinic.model.Appointment.Status.*;

/**
 * Two independent HTTP requests call these two transaction boundaries.
 *
 * <p>The regular request locks one appointment, pauses, then tries
 * to commit. An emergency request can book the same appointment during that wait
 * if Oracle rolls back the LOW blocker. LOW discovers that rollback on its final
 * update (ORA-63300/63302). Letting the error leave this method makes Micronaut
 * acknowledge the rollback before returning the connection to the pool.</p>
 *
 * <p>The pause defaults to fifteen seconds and is configurable through
 * {@code petclinic.transaction-priority.reservation-seconds} so tests can run faster.
 * The thirty-second transaction timeout allows the default pause, up to ten
 * seconds acquiring the lock, and time to commit. The pause is solely
 * for this showcase; production booking transactions should finish promptly.
 * These two ordered appointments can later be extended into a scheduler.</p>
 */
@Singleton
@Requires(env = "oracle")
public class OracleTransactionPriorityService {
    private final AppointmentRepository appointments;
    private final int reservationSeconds;

    public OracleTransactionPriorityService(AppointmentRepository appointments,
                                            @Value("${petclinic.transaction-priority.reservation-seconds:15}") int reservationSeconds) {
        this.appointments = appointments;
        this.reservationSeconds = reservationSeconds;
    }

    /** Pause used by LOW and the page's estimated response countdown. */
    public int getReservationSeconds() {
        return reservationSeconds;
    }

    @OracleTransactional(priority = OracleTransactional.Priority.LOW, timeout = 30)
    public void bookRegular(Integer appointmentId) {
        Appointment appointment = lockAvailable(appointmentId);
        appointment = appointments.save(appointment.withStatus(HELD_BY_REGULAR));
        try {
            Thread.sleep(Duration.ofSeconds(reservationSeconds));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Regular booking was interrupted", e);
        }
        // If HIGH displaced this transaction, Oracle reports its rollback here.
        appointments.save(appointment.withStatus(BOOKED_FOR_REGULAR));
    }

    @OracleTransactional(priority = OracleTransactional.Priority.HIGH, timeout = 30)
    public void bookEmergency(Integer appointmentId) {
        Appointment appointment = lockAvailable(appointmentId);
        appointments.save(appointment.withStatus(BOOKED_FOR_EMERGENCY));
    }

    @Transactional
    public void resetFixture() {
        // Fail immediately if a booking still holds either row; do not cancel it.
        for (Appointment appointment : appointments.findAll()) {
            appointments.save(appointment.withStatus(AVAILABLE));
        }
    }

    private Appointment lockAvailable(Integer appointmentId) {
        Appointment appointment = appointments.findById(appointmentId).orElseThrow(BookingTaken::new);
        if (appointment.status() != AVAILABLE) throw new BookingTaken();
        return appointment;
    }

    public static class BookingTaken extends RuntimeException {
        public BookingTaken() { super("The appointment has already been booked"); }
    }
}
