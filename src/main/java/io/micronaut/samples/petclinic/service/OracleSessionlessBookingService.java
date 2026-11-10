package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.dto.SessionlessBookingResult;
import io.micronaut.samples.petclinic.exception.AppointmentUnavailableException;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.oracle.OracleErrorCode;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.transaction.exceptions.TransactionTimedOutException;
import io.micronaut.transaction.jdbc.oracle.OracleSessionlessTransactionPropagationOperations;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static io.micronaut.samples.petclinic.dto.SessionlessBookingResult.Outcome.CANCELLED;
import static io.micronaut.samples.petclinic.dto.SessionlessBookingResult.Outcome.CONFIRMED;
import static io.micronaut.samples.petclinic.dto.SessionlessBookingResult.Outcome.HELD;
import static io.micronaut.samples.petclinic.exception.AppointmentUnavailableException.Reason;
import static io.micronaut.samples.petclinic.model.Appointment.Scenario.SESSIONLESS;

/** Coordinates sessionless booking requests and reads state after transaction completion. */
@Singleton
@Requires(env = "oracle")
public class OracleSessionlessBookingService {
    /** Maximum time in seconds that Oracle keeps a suspended hold alive. */
    public static final int HOLD_SECONDS = 60;

    private final AppointmentRepository appointmentRepository;
    private final OracleSessionlessBookingTransactionsService bookingTransactions;
    private final OracleSessionlessTransactionPropagationOperations propagation;

    /**
     * Creates the service that coordinates sessionless transactions and propagation.
     *
     * @param appointmentRepository appointment queries for committed showcase state
     * @param bookingTransactions intercepted transaction boundaries
     * @param propagation Oracle sessionless transaction propagation operations
     */
    public OracleSessionlessBookingService(AppointmentRepository appointmentRepository,
                                           OracleSessionlessBookingTransactionsService bookingTransactions,
                                           OracleSessionlessTransactionPropagationOperations propagation) {
        this.appointmentRepository = appointmentRepository;
        this.bookingTransactions = bookingTransactions;
        this.propagation = propagation;
    }

    /**
     * Lists appointments belonging to a showcase, in display order.
     *
     * @param scenario showcase to list
     * @return appointments in display order
     */
    public List<Appointment> findByScenario(Appointment.Scenario scenario) {
        return appointmentRepository.findByScenarioOrderByDisplayOrderAscAndIdAsc(scenario);
    }

    /**
     * Holds an appointment and returns its handle after suspension releases the connection.
     * Acquires the row lock only inside the intercepted sessionless transaction.
     *
     * @param appointmentId identifier of the appointment to hold
     * @return the suspended transaction handle and committed appointment state
     * @throws NotFoundEntityException if the sessionless appointment does not exist
     * @throws AppointmentUnavailableException if the appointment is booked or locked by another request
     * @throws IllegalRequestArgumentException if transaction propagation cannot be started
     */
    public SessionlessBookingResult hold(Integer appointmentId)
            throws IllegalRequestArgumentException {
        bookingTransactions.findAppointmentById(appointmentId);
         String transactionId = Objects.requireNonNull(propagate(null, () -> bookingTransactions.hold(appointmentId)),
                "A suspended booking must have a transaction ID");
        return result(appointmentId, HELD, transactionId);
    }

    /**
     * Resumes and commits the hold before reading the confirmed appointment state.
     *
     * @param appointmentId identifier of the held appointment
     * @param transactionId opaque handle returned by {@link #hold(Integer)}
     * @return the confirmed result and committed appointment state
     * @throws NotFoundEntityException if the appointment does not belong to the sessionless showcase
     * @throws AppointmentUnavailableException if the hold is expired, completed, or busy
     * @throws IllegalRequestArgumentException if the transaction handle is missing or malformed
     */
    public SessionlessBookingResult confirm(Integer appointmentId, String transactionId)
            throws IllegalRequestArgumentException {
        return complete(appointmentId, transactionId, true);
    }

    /**
     * Resumes and rolls back the hold before reading committed appointment state.
     *
     * @param appointmentId identifier of the held appointment
     * @param transactionId opaque handle returned by {@link #hold(Integer)}
     * @return the cancellation result and committed appointment state
     * @throws NotFoundEntityException if the appointment does not belong to the sessionless showcase
     * @throws AppointmentUnavailableException if the hold is expired, completed, or busy
     * @throws IllegalRequestArgumentException if the transaction handle is missing or malformed
     */
    public SessionlessBookingResult cancel(Integer appointmentId, String transactionId)
            throws IllegalRequestArgumentException {
        return complete(appointmentId, transactionId, false);
    }

    /**
     * Resets completed bookings in an intercepted transaction; leaves suspended holds alone.
     *
     * @throws NotFoundEntityException if an appointment disappears during the reset
     * @throws AppointmentUnavailableException if a booking or lock prevents the reset
     */
    public void resetFixture() throws NotFoundEntityException, AppointmentUnavailableException {
        try {
            bookingTransactions.resetFixture();
        } catch (RuntimeException error) {
            throw translateFailure(error);
        }
    }

    private SessionlessBookingResult complete(Integer appointmentId, String transactionId, boolean confirm)
            throws IllegalRequestArgumentException {
        bookingTransactions.findAppointmentById(appointmentId);
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalRequestArgumentException("The transaction ID is required.");
        }
        propagate(transactionId, () -> {
            if (confirm) bookingTransactions.confirm(appointmentId);
            else bookingTransactions.cancel(appointmentId);
        });
        return result(appointmentId, confirm ? CONFIRMED : CANCELLED, null);
    }

    private SessionlessBookingResult result(Integer appointmentId,
                                            SessionlessBookingResult.Outcome outcome,
                                            @Nullable String transactionId) throws NotFoundEntityException {
        Appointment appointment = bookingTransactions.findAppointmentById(appointmentId);
        return new SessionlessBookingResult(appointmentId, outcome, transactionId, HOLD_SECONDS,
                appointment.status(), findByScenario(SESSIONLESS));
    }

    private @Nullable String propagate(@Nullable String transactionId, BookingWork work) throws IllegalRequestArgumentException {
        // The propagation API accepts a Supplier. Capture checked business failures only
        // after the transaction proxy has rolled back, then rethrow outside the scope.
        Supplier<PropagationResult> operation = () -> {
            try {
                work.run();
                return new PropagationResult(propagation.currentTransactionId().orElse(null), null);
            } catch (NotFoundEntityException | AppointmentUnavailableException error) {
                return new PropagationResult(null, error);
            }
        };
        PropagationResult result;
        try {
            result = transactionId == null ? propagation.withPropagation(operation) : propagation.withPropagation(transactionId, operation);
            if (result.failure != null) {
                throw result.failure();
            }
        } catch (IllegalArgumentException error) {
            throw new IllegalRequestArgumentException("The transaction ID is malformed.");
        } catch (RuntimeException error) {
            throw translateFailure(error);
        }
        return result.transactionId();
    }

    private static AppointmentUnavailableException translateFailure(RuntimeException error) {
        Reason reason = unavailabilityReason(error);
        if (reason == null) throw error;
        return new AppointmentUnavailableException(reason, error);
    }

    /**
     * Identifies known Oracle failures that represent an unavailable or busy hold.
     *
     * @param error runtime failure returned by transaction propagation or JDBC
     * @return the corresponding availability reason, or {@code null} for unrelated failures
     */
    @Nullable
    static Reason unavailabilityReason(RuntimeException error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof TransactionTimedOutException) {
                return Reason.BUSY;
            }
            if (cause instanceof SQLException sql) {
                // A pipelined resume can report its SQL failure in the JDBC next-exception chain.
                for (SQLException current = sql; current != null; current = current.getNextException()) {
                    OracleErrorCode code = OracleErrorCode.fromCode(current.getErrorCode());
                    if (code == null) continue;
                    switch (code) {
                        case ORA_26218_SESSIONLESS_TRANSACTION_DOES_NOT_EXIST,
                             ORA_24756_TRANSACTION_DOES_NOT_EXIST -> { return Reason.UNAVAILABLE; }
                        case ORA_00054_RESOURCE_BUSY,
                             ORA_01013_USER_REQUESTED_CANCEL,
                             ORA_25351_TRANSACTION_ATTACHED_TO_OTHER_SESSION,
                             ORA_30006_RESOURCE_BUSY_WAIT_TIMEOUT -> { return Reason.BUSY; }
                        default -> { }
                    }
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface BookingWork {
        void run() throws NotFoundEntityException, AppointmentUnavailableException;
    }

    private record PropagationResult(@Nullable String transactionId, @Nullable IllegalRequestArgumentException failure) {
    }
}
