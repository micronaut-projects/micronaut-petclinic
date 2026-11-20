package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Status;
import io.micronaut.samples.petclinic.dto.SessionlessBookingRequest;
import io.micronaut.samples.petclinic.dto.SessionlessBookingResult;
import io.micronaut.samples.petclinic.exception.AppointmentUnavailableException;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.service.OracleSessionlessBookingService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

import static io.micronaut.samples.petclinic.model.Appointment.Scenario.SESSIONLESS;

/** HTTP endpoints for the Oracle sessionless booking example. */
@Controller("/oracle/transaction-priority/sessionless")
@Requires(env = "oracle")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_ANONYMOUS)
public class OracleSessionlessBookingController {
    private final OracleSessionlessBookingService service;

    /**
     * Creates the sessionless booking controller.
     *
     * @param service booking operations and committed-state queries
     */
    public OracleSessionlessBookingController(OracleSessionlessBookingService service) {
        this.service = service;
    }

    /**
     * Lists the committed appointment states for the sessionless showcase.
     *
     * @return sessionless appointments in display order
     */
    @Get("/appointments")
    public List<Appointment> appointments() {
        return service.findByScenario(SESSIONLESS);
    }

    /**
     * Starts a sessionless transaction that holds the requested appointment.
     *
     * @param appointmentId identifier of the appointment to hold
     * @return the suspended transaction handle and committed appointment state
     * @throws NotFoundEntityException if the appointment is missing or belongs to another showcase
     * @throws AppointmentUnavailableException if the appointment is booked or locked by another request
     * @throws IllegalRequestArgumentException if the request or transaction setup is invalid
     */
    @Post("/hold")
    @Status(HttpStatus.CREATED)
    public SessionlessBookingResult hold(@Positive @QueryValue Integer appointmentId)
            throws NotFoundEntityException, IllegalRequestArgumentException {
        return service.hold(appointmentId);
    }

    /**
     * Resumes a suspended hold and commits the booking.
     *
     * @param request appointment identifier and suspended transaction handle
     * @return completion result and committed appointment state
     * @throws NotFoundEntityException if the appointment is missing or belongs to another showcase
     * @throws AppointmentUnavailableException if the hold is unavailable or busy
     * @throws IllegalRequestArgumentException if the transaction handle is missing or malformed
     */
    @Post("/confirm")
    public SessionlessBookingResult confirm(@Valid @Body SessionlessBookingRequest request)
            throws NotFoundEntityException, IllegalRequestArgumentException {
        return service.confirm(request.appointmentId(), request.transactionId());
    }

    /**
     * Resumes a suspended hold and rolls back the booking.
     *
     * @param request appointment identifier and suspended transaction handle
     * @return cancellation result and committed appointment state
     * @throws NotFoundEntityException if the appointment is missing or belongs to another showcase
     * @throws AppointmentUnavailableException if the hold is unavailable or busy
     * @throws IllegalRequestArgumentException if the transaction handle is missing or malformed
     */
    @Post("/cancel")
    public SessionlessBookingResult cancel(@Valid @Body SessionlessBookingRequest request)
            throws NotFoundEntityException, IllegalRequestArgumentException {
        return service.cancel(request.appointmentId(), request.transactionId());
    }

    /**
     * Resets completed sessionless bookings to available.
     *
     * @return committed sessionless appointment states after reset
     * @throws NotFoundEntityException if an appointment disappears during the reset
     * @throws AppointmentUnavailableException if a booking or lock prevents the reset
     */
    @Post("/reset")
    public List<Appointment> reset() throws NotFoundEntityException, AppointmentUnavailableException {
        service.resetFixture();
        return service.findByScenario(SESSIONLESS);
    }

    /**
     * Converts an unavailable appointment failure to this endpoint's conflict response.
     *
     * @param error the reason the appointment or suspended transaction could not be used
     * @return HTTP 409 with a reason code and English message
     */
    @Error(exception = AppointmentUnavailableException.class)
    public HttpResponse<Map<String, String>> unavailable(AppointmentUnavailableException error) {
        String message = error.getMessage() == null || error.getMessage().isBlank()
                ? "The appointment or transaction is unavailable."
                : error.getMessage();
        return HttpResponse.<Map<String, String>>status(HttpStatus.CONFLICT)
                .body(Map.of("code", error.getReason().name(), "message", message));
    }
}
