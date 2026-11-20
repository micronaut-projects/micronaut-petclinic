package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.samples.petclinic.dto.OracleBookingResult;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.oracle.OracleErrorCode;
import io.micronaut.samples.petclinic.exception.AppointmentUnavailableException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.service.OracleTransactionPriorityService;
import io.micronaut.samples.petclinic.service.OracleSessionlessBookingService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.transaction.exceptions.OracleTransactionPriorityException;
import io.micronaut.transaction.exceptions.TransactionTimedOutException;
import io.micronaut.views.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;

import static io.micronaut.samples.petclinic.dto.OracleBookingResult.Outcome;

/** Each POST owns its transaction. Blocking work runs off the HTTP event loop. */
@Controller("/oracle/transaction-priority")
@Requires(env = "oracle")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_ANONYMOUS)
public class OracleTransactionPriorityController {

    private static final Logger LOG = LoggerFactory.getLogger(OracleTransactionPriorityController.class);
    private final OracleTransactionPriorityService service;
    private final OracleSessionlessBookingService sessionlessService;

    /**
     * Creates the controller for both Oracle transaction showcases.
     *
     * @param service priority-booking operations
     * @param sessionlessService sessionless-booking operations and committed-state queries
     */
    public OracleTransactionPriorityController(OracleTransactionPriorityService service,
                                               OracleSessionlessBookingService sessionlessService) {
        this.service = service;
        this.sessionlessService = sessionlessService;
    }

    /**
     * Maps booking failures to the outcome shown by the priority showcase.
     *
     * @param error booking failure to classify
     * @return the displayed booking outcome
     */
    static Outcome outcomeOf(Exception error) {
        return switch (error) {
            case OracleTransactionPriorityException _ -> Outcome.PRIORITY_ROLLED_BACK;
            case AppointmentUnavailableException _ -> Outcome.TAKEN;
            case TransactionTimedOutException _ -> Outcome.TIMED_OUT;
            case DataAccessException data when data.getCause() instanceof SQLException sql -> {
                OracleErrorCode code = OracleErrorCode.fromCode(sql.getErrorCode());
                if (code == null) yield Outcome.FAILED;
                yield switch (code) {
                    case ORA_00054_RESOURCE_BUSY,
                         ORA_01013_USER_REQUESTED_CANCEL,
                         ORA_30006_RESOURCE_BUSY_WAIT_TIMEOUT -> Outcome.TIMED_OUT;
                    default -> Outcome.FAILED;
                };
            }
            default -> Outcome.FAILED;
        };
    }

    /**
     * Renders the priority and sessionless booking examples with their current state.
     *
     * @return model used by the transaction showcase page
     */
    @Get
    @View("transaction-priority/showcase")
    @Produces(MediaType.TEXT_HTML)
    public Map<String, Object> page() {
        return Map.of("appointments", service.findAvailableAppointments(),
                "reservationSeconds", service.getReservationSeconds(),
                "sessionlessAppointments", sessionlessService.findByScenario(Appointment.Scenario.SESSIONLESS),
                "holdSeconds", OracleSessionlessBookingService.HOLD_SECONDS);
    }

    /**
     * Resets completed priority bookings.
     *
     * @return reset status
     * @throws HttpStatusException if a booking prevents reset from acquiring its lock in time
     */
    @Post("/reset")
    public Map<String, String> reset() {
        try {
            service.resetFixture();
        } catch (RuntimeException e) {
            if (outcomeOf(e) == Outcome.TIMED_OUT) {
                throw new HttpStatusException(HttpStatus.CONFLICT, "A booking is still running. Wait for it to finish before resetting.");
            }
            throw e;
        }
        return Map.of("status", "reset");
    }

    /**
     * Books an appointment using the selected priority.
     *
     * @param appointmentId identifier of the priority appointment
     * @param type regular (LOW) or emergency (HIGH) booking
     * @return booking outcome and remaining available appointments
     * @throws NotFoundEntityException if the appointment is missing or belongs to another showcase
     */
    @Post("/book")
    public HttpResponse<OracleBookingResult> book(@QueryValue Integer appointmentId,
                                                  @QueryValue BookingType type) throws NotFoundEntityException {
        service.findById(appointmentId);
        Outcome outcome = Outcome.COMMITTED;
        try {
            // Returns only after the transaction interceptor commits or rolls back.
            switch (type) {
                case REGULAR -> service.bookRegular(appointmentId);
                case EMERGENCY -> service.bookEmergency(appointmentId);
            }
        } catch (AppointmentUnavailableException | RuntimeException e) {
            outcome = outcomeOf(e);
            if (outcome == Outcome.FAILED) {
                LOG.error("Appointment booking failed for appointmentId={}", appointmentId, e);
            }
        }
        var booked = service.findById(appointmentId);
        var result = new OracleBookingResult(appointmentId, outcome, booked.status(),
                service.findAvailableAppointments());
        HttpStatus status = switch (outcome) {
            case COMMITTED -> HttpStatus.OK;
            case PRIORITY_ROLLED_BACK, TAKEN -> HttpStatus.CONFLICT;
            case TIMED_OUT -> HttpStatus.SERVICE_UNAVAILABLE;
            case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return HttpResponse.status(status).body(result);
    }

    /**
     * Returns an HTTP error raised by a priority-showcase request.
     *
     * @param error status and message to return
     * @return response with the same status and message
     */
    @Error(exception = HttpStatusException.class)
    public HttpResponse<Map<String, String>> labError(HttpStatusException error) {
        return HttpResponse.status(error.getStatus()).body(Map.of("message", error.getMessage()));
    }

    /** Booking priority selected by the showcase request. */
    public enum BookingType {
        /** Uses Oracle's LOW transaction priority. */
        REGULAR,
        /** Uses Oracle's HIGH transaction priority. */
        EMERGENCY
    }
}
