package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.exception.AppointmentUnavailableException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.transaction.TransactionOperations;
import io.micronaut.transaction.annotation.OracleTransactional;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.sql.Connection;

import static io.micronaut.samples.petclinic.model.Appointment.Scenario.SESSIONLESS;
import static io.micronaut.samples.petclinic.model.Appointment.Status.*;
import static io.micronaut.transaction.annotation.OracleTransactional.Sessionless.*;

/**
 * Transaction boundaries invoked by {@link OracleSessionlessBookingService}.
 * A separate bean ensures suspension, commit, or rollback finishes before the service
 * exports the transaction ID or reads committed state. Checked domain failures roll back too.
 */
@Singleton
@Requires(env = "oracle")
public class OracleSessionlessBookingTransactionsService {
    private final AppointmentRepository appointmentRepository;
    private final TransactionOperations<Connection> transactions;

    /**
     * Creates the bean containing the intercepted transaction boundaries.
     *
     * @param appointmentRepository repository used inside each transaction
     * @param transactions operations used to mark a transaction for rollback
     */
    public OracleSessionlessBookingTransactionsService(AppointmentRepository appointmentRepository,
                                                       TransactionOperations<Connection> transactions) {
        this.appointmentRepository = appointmentRepository;
        this.transactions = transactions;
    }

    /**
     * Finds a sessionless appointment without acquiring a booking lock.
     *
     * @param id appointment identifier
     * @return the appointment when it belongs to the sessionless showcase
     * @throws NotFoundEntityException if the appointment is missing or belongs to another scenario
     */
    public Appointment findAppointmentById(@NotNull @Positive Integer id) throws NotFoundEntityException {
        return appointmentRepository.findByIdAndScenario(id, SESSIONLESS)
                .orElseThrow(() -> new NotFoundEntityException("Appointment not found."));
    }

    /**
     * Locks an available appointment, marks it held, then suspends the transaction.
     *
     * @param appointmentId identifier of the appointment to hold
     * @throws NotFoundEntityException if the appointment is missing or belongs to another scenario
     * @throws AppointmentUnavailableException if the appointment is not available
     */
    @OracleTransactional(sessionless = SUSPEND, timeout = OracleSessionlessBookingService.HOLD_SECONDS,
            rollbackFor = Exception.class)
    public void hold(Integer appointmentId) throws NotFoundEntityException, AppointmentUnavailableException {
        Appointment appointment = findByIdForUpdate(appointmentId);
        if (appointment.status() != AVAILABLE) throw new AppointmentUnavailableException();
        appointmentRepository.save(appointment.withStatus(HELD_FOR_CONFIRMATION));
    }

    /**
     * Confirms the held appointment in the resumed transaction.
     *
     * @param appointmentId identifier of the held appointment
     * @throws NotFoundEntityException if the appointment is missing or belongs to another scenario
     * @throws AppointmentUnavailableException if the appointment is not held for confirmation
     */
    @OracleTransactional(sessionless = REQUIRES_SUSPENDED, rollbackFor = Exception.class)
    public void confirm(Integer appointmentId) throws NotFoundEntityException, AppointmentUnavailableException {
        Appointment appointment = findAppointmentById(appointmentId);
        heldAppointment(appointment);
        appointmentRepository.save(appointment.withStatus(BOOKED_AFTER_CONFIRMATION));
    }

    /**
     * Rolls back the original hold instead of committing an {@code AVAILABLE} update.
     *
     * @param appointmentId identifier of the held appointment
     * @throws NotFoundEntityException if the appointment is missing or belongs to another scenario
     * @throws AppointmentUnavailableException if the appointment is not held for confirmation
     */
    @OracleTransactional(sessionless = REQUIRES_SUSPENDED, rollbackFor = Exception.class)
    public void cancel(Integer appointmentId) throws NotFoundEntityException, AppointmentUnavailableException {
        Appointment appointment = findAppointmentById(appointmentId);
        heldAppointment(appointment);
        transactions.findTransactionStatus().orElseThrow().setRollbackOnly();
    }

    /**
     * Resets confirmed sessionless bookings, leaving suspended holds alone.
     * Pending holds still read as {@code AVAILABLE} to this transaction.
     *
     * @throws NotFoundEntityException if a confirmed appointment disappears while being reset
     */
    @Transactional(rollbackOn = Exception.class)
    public void resetFixture() throws NotFoundEntityException {
        for (Appointment appointment : appointmentRepository.findByScenarioAndStatusOrderByDisplayOrderAscAndIdAsc(
                SESSIONLESS, BOOKED_AFTER_CONFIRMATION)) {
            Appointment locked = findByIdForUpdate(appointment.id());
            if (locked.status() == BOOKED_AFTER_CONFIRMATION) {
                appointmentRepository.save(locked.withStatus(AVAILABLE));
            }
        }
    }

    private Appointment findByIdForUpdate(Integer appointmentId) throws NotFoundEntityException {
        return appointmentRepository.findByIdAndScenarioForUpdate(appointmentId, SESSIONLESS)
                .orElseThrow(() -> new NotFoundEntityException("Appointment not found."));
    }


    private void heldAppointment(Appointment appointment)
            throws AppointmentUnavailableException {
        if (appointment.status() != HELD_FOR_CONFIRMATION) {
            throw new AppointmentUnavailableException();
        }
    }
}
