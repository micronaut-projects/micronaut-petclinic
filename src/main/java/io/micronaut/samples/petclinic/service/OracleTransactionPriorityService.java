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
 * Demonstrates Oracle transaction priority with separate regular (LOW) and emergency (HIGH) requests.
 *
 * <p>LOW locks an appointment and pauses. HIGH can trigger Oracle to roll LOW back
 * and book that appointment. LOW detects the rollback on its final update;
 * Micronaut handles it and throws
 * {@link io.micronaut.transaction.exceptions.OracleTransactionPriorityException}.</p>
 *
 * <p>The demo-only pause defaults to 15 seconds, configured by
 * {@code petclinic.transaction-priority.reservation-seconds}. Both transactions
 * time out after 30 seconds. Production bookings should not pause.</p>
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

    /**
     * Makes all appointments available. Use only after booking requests finish:
     * reset runs at Oracle's default HIGH priority and can trigger rollback of a
     * blocking LOW booking when priority rollback is enabled. It can also clear
     * a booking that commits while reset waits for its row lock.
     */
    @Transactional
    public void resetFixture() {
        for (Appointment appointment : appointments.findAll()) {
            appointments.save(appointment.withStatus(AVAILABLE));
        }
    }

    private Appointment lockAvailable(Integer appointmentId) {
        Appointment appointment = appointments.findByIdForUpdate(appointmentId).orElseThrow(BookingTaken::new);
        if (appointment.status() != AVAILABLE) throw new BookingTaken();
        return appointment;
    }

    public static class BookingTaken extends RuntimeException {
        public BookingTaken() { super("The appointment has already been booked"); }
    }
}
