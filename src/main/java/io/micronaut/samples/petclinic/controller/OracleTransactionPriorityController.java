package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Requires;
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
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.samples.petclinic.service.OracleTransactionPriorityService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.views.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import static io.micronaut.samples.petclinic.dto.OracleBookingResult.Outcome;

/** Each POST owns its transaction. Blocking work runs off the HTTP event loop. */
@Controller("/oracle/transaction-priority")
@Requires(env = "oracle")
@ExecuteOn(TaskExecutors.BLOCKING)
public class OracleTransactionPriorityController {

    private static final Logger LOG = LoggerFactory.getLogger(OracleTransactionPriorityController.class);
    private final OracleTransactionPriorityService service;
    private final AppointmentRepository appointments;

    public OracleTransactionPriorityController(OracleTransactionPriorityService service,
                                               AppointmentRepository appointments) {
        this.service = service;
        this.appointments = appointments;
    }

    /** Only Oracle error codes confirm a priority rollback, never request timing. */
    static Outcome outcomeOf(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof OracleTransactionPriorityService.BookingTaken) {
                return Outcome.TAKEN;
            }
            if (cause instanceof SQLException sql) {
                Set<SQLException> sqlSeen = Collections.newSetFromMap(new IdentityHashMap<>());
                for (; sql != null && sqlSeen.add(sql); sql = sql.getNextException()) {
                    if (sql.getErrorCode() == 63300 || sql.getErrorCode() == 63302) {
                        return Outcome.PRIORITY_ROLLED_BACK;
                    }
                    if (sql.getErrorCode() == 54 || sql.getErrorCode() == 30006 || sql.getErrorCode() == 1013) {
                        return Outcome.TIMED_OUT;
                    }
                }
            }
        }
        return Outcome.FAILED;
    }

    @Get
    @View("transaction-priority/showcase")
    @Produces(MediaType.TEXT_HTML)
    public Map<String, Object> page() {
        return Map.of("appointments", appointments.findAvailableAppointments(),
                "reservationSeconds", service.getReservationSeconds());
    }

    @Post("/reset")
    @Produces(MediaType.APPLICATION_JSON)
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

    @Post("/book")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<OracleBookingResult> book(@QueryValue Integer appointmentId,
                                                  @QueryValue BookingType type) {
        Runnable transaction = switch (type) {
            case REGULAR -> () -> service.bookRegular(appointmentId);
            case EMERGENCY -> () -> service.bookEmergency(appointmentId);
        };
        return executeBooking(appointmentId, transaction);
    }

    private HttpResponse<OracleBookingResult> executeBooking(Integer appointmentId, Runnable transaction) {
        if (!appointments.existsById(appointmentId)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Appointment not found.");
        }
        Outcome outcome = Outcome.COMMITTED;
        try {
            transaction.run(); // Returns only after the transaction interceptor commits or rolls back.
        } catch (RuntimeException e) {
            outcome = outcomeOf(e);
            if (outcome == Outcome.FAILED) {
                LOG.error("Appointment booking failed", e);
            }
        }
        var booked = appointments.findById(appointmentId)
                .orElseThrow(() -> new HttpStatusException(HttpStatus.NOT_FOUND, "Appointment not found."));
        var result = new OracleBookingResult(appointmentId, outcome, booked.status(),
                appointments.findAvailableAppointments());
        HttpStatus status = switch (outcome) {
            case COMMITTED -> HttpStatus.OK;
            case PRIORITY_ROLLED_BACK, TAKEN -> HttpStatus.CONFLICT;
            case TIMED_OUT -> HttpStatus.SERVICE_UNAVAILABLE;
            case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return HttpResponse.status(status).body(result);
    }

    @Error(exception = HttpStatusException.class)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<Map<String, String>> labError(HttpStatusException error) {
        return HttpResponse.status(error.getStatus()).body(Map.of("message", error.getMessage()));
    }

    public enum BookingType {
        REGULAR,
        EMERGENCY
    }
}
