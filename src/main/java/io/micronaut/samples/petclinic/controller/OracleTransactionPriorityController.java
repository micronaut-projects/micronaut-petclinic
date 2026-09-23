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
import io.micronaut.samples.petclinic.service.OracleTransactionPriorityWorker;
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
import static io.micronaut.samples.petclinic.model.Appointment.Status.AVAILABLE;

/** Each POST owns its transaction. Blocking work runs off the HTTP event loop. */
@Controller("/oracle/transaction-priority")
@Requires(env = "oracle")
@ExecuteOn(TaskExecutors.BLOCKING)
public class OracleTransactionPriorityController {
    private static final Logger LOG = LoggerFactory.getLogger(OracleTransactionPriorityController.class);
    private final OracleTransactionPriorityWorker worker;
    private final AppointmentRepository appointments;

    public OracleTransactionPriorityController(OracleTransactionPriorityWorker worker,
                                               AppointmentRepository appointments) {
        this.worker = worker;
        this.appointments = appointments;
    }

    @Get
    @View("transaction-priority/showcase")
    @Produces(MediaType.TEXT_HTML)
    public Map<String, Object> page() {
        return Map.of("appointments", appointments.findDemoAppointments().stream()
                        .filter(a -> a.status() == AVAILABLE).toList(),
                "reservationSeconds", worker.getReservationSeconds());
    }

    @Post("/regular")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<OracleBookingResult> regular(@QueryValue Integer appointmentId) {
        return book(appointmentId, () -> worker.bookRegular(appointmentId));
    }

    @Post("/emergency")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<OracleBookingResult> emergency(@QueryValue Integer appointmentId) {
        return book(appointmentId, () -> worker.bookEmergency(appointmentId));
    }

    @Post("/reset")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> reset() {
        try {
            worker.resetFixture();
        } catch (RuntimeException e) {
            if (outcomeOf(e) == Outcome.TIMED_OUT) {
                throw new HttpStatusException(HttpStatus.CONFLICT, "A booking is still running. Wait for it to finish before resetting.");
            }
            throw e;
        }
        return Map.of("status", "reset");
    }

    private HttpResponse<OracleBookingResult> book(Integer appointmentId, Runnable transaction) {
        if (appointments.findDemoAppointments().stream().noneMatch(a -> a.id().equals(appointmentId))) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "This is not a demo appointment.");
        }
        Outcome outcome = Outcome.COMMITTED;
        try {
            transaction.run(); // Returns only after the transaction interceptor commits or rolls back.
        } catch (RuntimeException e) {
            outcome = outcomeOf(e);
            if (outcome == Outcome.FAILED) LOG.error("Appointment booking failed", e);
        }
        var fixture = appointments.findDemoAppointments();
        var booked = fixture.stream().filter(a -> a.id().equals(appointmentId)).findFirst().orElseThrow();
        var result = new OracleBookingResult(appointmentId, outcome, booked.status(),
                fixture.stream().filter(a -> a.status() == AVAILABLE).toList());
        HttpStatus status = switch (outcome) {
            case COMMITTED -> HttpStatus.OK;
            case PRIORITY_ROLLED_BACK, TAKEN -> HttpStatus.CONFLICT;
            case TIMED_OUT -> HttpStatus.SERVICE_UNAVAILABLE;
            case FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return HttpResponse.status(status).body(result);
    }

    /** Only Oracle error codes confirm a priority rollback, never request timing. */
    static Outcome outcomeOf(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof OracleTransactionPriorityWorker.BookingTaken) return Outcome.TAKEN;
            if (cause instanceof SQLException sql) {
                Set<SQLException> sqlSeen = Collections.newSetFromMap(new IdentityHashMap<>());
                for (; sql != null && sqlSeen.add(sql); sql = sql.getNextException()) {
                    if (sql.getErrorCode() == 63300 || sql.getErrorCode() == 63302) return Outcome.PRIORITY_ROLLED_BACK;
                    if (sql.getErrorCode() == 54 || sql.getErrorCode() == 30006 || sql.getErrorCode() == 1013) return Outcome.TIMED_OUT;
                }
            }
        }
        return Outcome.FAILED;
    }

    @Error(exception = HttpStatusException.class)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<Map<String, String>> labError(HttpStatusException error) {
        return HttpResponse.status(error.getStatus()).body(Map.of("message", error.getMessage()));
    }
}
