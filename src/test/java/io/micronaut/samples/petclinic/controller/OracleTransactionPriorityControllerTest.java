package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.samples.petclinic.dto.OracleBookingResult;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.samples.petclinic.repository.OracleAppointmentLockRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.micronaut.samples.petclinic.dto.OracleBookingResult.Outcome.COMMITTED;
import static io.micronaut.samples.petclinic.dto.OracleBookingResult.Outcome.PRIORITY_ROLLED_BACK;
import static io.micronaut.samples.petclinic.dto.OracleBookingResult.Outcome.TAKEN;
import static io.micronaut.samples.petclinic.model.Appointment.Status.BOOKED_FOR_EMERGENCY;
import static io.micronaut.samples.petclinic.model.Appointment.Status.BOOKED_FOR_REGULAR;
import static io.micronaut.samples.petclinic.support.OracleTransactionPriorityTestSupport.awaitRegularLock;
import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end booking requests through the embedded HTTP server and a real Oracle database. */
@MicronautTest(transactional = false)
@Requires(env = "oracle")
@Property(name = "petclinic.transaction-priority.reservation-seconds", value = "5")
@Property(name = "micronaut.http.client.exception-on-error-status", value = "false")
@Property(name = "micronaut.http.client.read-timeout", value = "40s")
class OracleTransactionPriorityControllerTest {

    @Inject AppointmentRepository appointments;
    @Inject OracleAppointmentLockRepository appointmentLocks;
    @Inject @Client("/") HttpClient client;

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
    void regularBookingReturnsHttp200AfterCommit() {
        var response = postBooking("regular");

        assertBookingResponse(response, HttpStatus.OK, COMMITTED, BOOKED_FOR_REGULAR);
        assertThat(appointments.findById(originalAppointment.id()).orElseThrow().status()).isEqualTo(BOOKED_FOR_REGULAR);
    }

    @Test
    void priorityRollbackReturnsHttp409AndEmergencyReturnsHttp200() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var regular = executor.submit(() -> postBooking("regular"));
            awaitRegularLock(appointmentLocks, originalAppointment.id());
            var emergency = postBooking("emergency");
            var rolledBack = regular.get(30, TimeUnit.SECONDS);

            assertBookingResponse(emergency, HttpStatus.OK, COMMITTED, BOOKED_FOR_EMERGENCY);
            assertBookingResponse(rolledBack, HttpStatus.CONFLICT, PRIORITY_ROLLED_BACK, BOOKED_FOR_EMERGENCY);
            assertThat(appointments.findById(originalAppointment.id()).orElseThrow().status()).isEqualTo(BOOKED_FOR_EMERGENCY);
        }
    }

    @Test
    void alreadyBookedAppointmentReturnsHttp409() {
        assertBookingResponse(postBooking("emergency"), HttpStatus.OK, COMMITTED, BOOKED_FOR_EMERGENCY);

        var response = postBooking("regular");

        assertBookingResponse(response, HttpStatus.CONFLICT, TAKEN, BOOKED_FOR_EMERGENCY);
        assertThat(appointments.findById(originalAppointment.id()).orElseThrow().status()).isEqualTo(BOOKED_FOR_EMERGENCY);
    }

    private HttpResponse<OracleBookingResult> postBooking(String type) {
        return client.toBlocking().exchange(
                HttpRequest.POST("/oracle/transaction-priority/book?appointmentId=" + originalAppointment.id()
                        + "&type=" + type, "").accept(MediaType.APPLICATION_JSON_TYPE),
                Argument.of(OracleBookingResult.class), Argument.of(OracleBookingResult.class));
    }

    private void assertBookingResponse(HttpResponse<OracleBookingResult> response, HttpStatus status,
                                       OracleBookingResult.Outcome outcome, Appointment.Status databaseStatus) {
        assertThat(response.code()).isEqualTo(status.getCode());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        var body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.appointmentId()).isEqualTo(originalAppointment.id());
        assertThat(body.outcome()).isEqualTo(outcome);
        assertThat(body.databaseStatus()).isEqualTo(databaseStatus);
    }
}
