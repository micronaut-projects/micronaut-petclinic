package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.samples.petclinic.exception.AppointmentUnavailableException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.transaction.annotation.OracleTransactional;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.util.List;

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

    /**
     * Creates the service that owns priority booking transactions.
     *
     * @param appointments appointment repository
     * @param reservationSeconds demo pause used by regular bookings
     */
    public OracleTransactionPriorityService(AppointmentRepository appointments,
                                            @Value("${petclinic.transaction-priority.reservation-seconds:15}") int reservationSeconds) {
        this.appointments = appointments;
        this.reservationSeconds = reservationSeconds;
    }

    /**
     * Returns the pause used by LOW and the page's estimated response countdown.
     *
     * @return regular-booking pause in seconds
     */
    public int getReservationSeconds() {
        return reservationSeconds;
    }

    /**
     * Attempts to book an available priority appointment at LOW priority after the demo pause.
     *
     * @param appointmentId identifier of the priority appointment
     * @throws NotFoundEntityException if the appointment is missing or belongs to another showcase
     * @throws AppointmentUnavailableException if the appointment is already booked
     * @throws IllegalStateException if the demo pause is interrupted
     * @throws io.micronaut.transaction.exceptions.OracleTransactionPriorityException if Oracle rolls back LOW
     */
    @OracleTransactional(priority = OracleTransactional.Priority.LOW, timeout = 30, rollbackFor = Exception.class)
    public void bookRegular(Integer appointmentId) throws NotFoundEntityException, AppointmentUnavailableException {
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

    /**
     * Books an available priority appointment at HIGH priority.
     *
     * @param appointmentId identifier of the priority appointment
     * @throws NotFoundEntityException if the appointment is missing or belongs to another showcase
     * @throws AppointmentUnavailableException if the appointment is already booked
     */
    @OracleTransactional(priority = OracleTransactional.Priority.HIGH, timeout = 30, rollbackFor = Exception.class)
    public void bookEmergency(Integer appointmentId) throws NotFoundEntityException, AppointmentUnavailableException {
        Appointment appointment = lockAvailable(appointmentId);
        appointments.save(appointment.withStatus(BOOKED_FOR_EMERGENCY));
    }

    /**
     * Makes priority appointments available. It runs at Oracle's default HIGH priority and,
     * when priority rollback is enabled, can trigger rollback of a blocking LOW booking.
     * It can also clear a booking that commits while reset waits for its row lock.
     *
     * <p>Use this only after booking requests finish.</p>
     */
    @Transactional
    public void resetFixture() {
        for (Appointment appointment : appointments.findByScenarioOrderByDisplayOrderAscAndIdAsc(Appointment.Scenario.PRIORITY)) {
            appointments.save(appointment.withStatus(AVAILABLE));
        }
    }

    /**
     * Lists available appointments in the priority showcase.
     *
     * @return available priority appointments in display order
     */
    public List<Appointment> findAvailableAppointments() {
        return appointments.findAvailableAppointments(Appointment.Scenario.PRIORITY);
    }

    /**
     * Finds an appointment only if it belongs to the priority showcase.
     *
     * @param appointmentId identifier of the appointment
     * @return the matching priority appointment
     * @throws NotFoundEntityException if the appointment is missing or belongs to another showcase
     */
    public Appointment findById(Integer appointmentId) throws NotFoundEntityException {
        return appointments.findByIdAndScenario(appointmentId, Appointment.Scenario.PRIORITY)
                .orElseThrow(() -> new NotFoundEntityException("Appointment not found."));
    }

    private Appointment lockAvailable(Integer appointmentId)
            throws NotFoundEntityException, AppointmentUnavailableException {
        Appointment appointment = appointments.findByIdAndScenarioForUpdate(appointmentId, Appointment.Scenario.PRIORITY)
                .orElseThrow(() -> new NotFoundEntityException("Appointment not found."));
        if (appointment.status() != AVAILABLE) throw new AppointmentUnavailableException();
        return appointment;
    }
}
