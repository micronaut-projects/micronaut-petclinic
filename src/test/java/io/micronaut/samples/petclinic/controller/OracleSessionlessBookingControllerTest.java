package io.micronaut.samples.petclinic.controller;

import com.zaxxer.hikari.HikariDataSource;
import io.micronaut.context.MessageSource;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.samples.petclinic.exception.AppointmentUnavailableException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.oracle.OracleErrorCode;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.samples.petclinic.repository.OracleAppointmentLockRepository;
import io.micronaut.samples.petclinic.service.OracleSessionlessBookingService;
import io.micronaut.samples.petclinic.service.OracleTransactionPriorityService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.transaction.jdbc.oracle.OracleSessionlessTransactionPropagationOperations;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import static io.micronaut.samples.petclinic.model.Appointment.Scenario.SESSIONLESS;
import static io.micronaut.samples.petclinic.model.Appointment.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Every HTTP request must finish and return the sole pooled connection before the next begins. */
@MicronautTest(transactional = false)
@Requires(env = "oracle")
@Property(name = "datasources.default.maximum-pool-size", value = "1")
@Property(name = "datasources.default.minimum-idle", value = "1")
@Property(name = "datasources.default.connection-timeout", value = "3000")
@Property(name = "micronaut.http.client.exception-on-error-status", value = "false")
@Property(name = "micronaut.http.client.read-timeout", value = "20s")
class OracleSessionlessBookingControllerTest {
    private static final String BASE = "/oracle/transaction-priority/sessionless";

    @Inject AppointmentRepository appointments;
    @Inject OracleAppointmentLockRepository locks;
    @Inject OracleSessionlessBookingService service;
    @Inject OracleTransactionPriorityService priority;
    @Inject OracleSessionlessTransactionPropagationOperations propagation;
    @Inject MessageSource messages;
    @Inject DataSource dataSource;
    @Inject DataSourceResolver dataSourceResolver;
    @Inject @Client("/") HttpClient client;

    private final List<Map<String, Object>> holds = new ArrayList<>();
    private Appointment first;
    private Appointment second;

    @BeforeEach
    void createFixtures() {
        first = appointments.save(new Appointment("Sessionless HTTP test", 90, SESSIONLESS));
        second = appointments.save(new Appointment("Second sessionless HTTP test", 91, SESSIONLESS));
    }

    @AfterEach
    void cleanUp() {
        for (var hold : holds) {
            try {
                service.cancel(((Number) hold.get("appointmentId")).intValue(), (String) hold.get("transactionId"));
            } catch (Exception ignored) {
                // Completed and expired transactions cannot be resumed again.
            }
        }
        holds.clear();
        appointments.deleteById(first.id());
        appointments.deleteById(second.id());
    }

    @Test
    void releasesTheOnlyConnectionAndCommitsTheResumedHold() {
        var hold = hold(first);
        assertThat(hold).containsEntry("databaseStatus", "AVAILABLE").containsEntry("holdSeconds", 60);
        assertThat(appointments.findById(first.id()).orElseThrow().status()).isEqualTo(AVAILABLE);
        assertThatThrownBy(() -> locks.lockNowait(first.id())).isInstanceOf(DataAccessException.class);
        assertThat(getAppointments()).contains("Sessionless HTTP test");

        var confirmed = post("/confirm", completion(hold));
        assertThat(confirmed.code()).isEqualTo(200);
        assertThat(confirmed.body()).containsEntry("outcome", "CONFIRMED")
                .containsEntry("databaseStatus", "BOOKED_AFTER_CONFIRMATION");
        assertThat(confirmed.body()).doesNotContainKey("transactionId");
        assertThat(appointments.findById(first.id()).orElseThrow().status()).isEqualTo(BOOKED_AFTER_CONFIRMATION);
        assertThat(locks.lockNowait(first.id())).isEqualTo(first.id());

        assertError(post("/confirm", completion(hold)), 409, "UNAVAILABLE");
        assertError(post("/hold?appointmentId=" + first.id(), ""), 409, "UNAVAILABLE");
        assertThatThrownBy(() -> service.hold(first.id())).isInstanceOf(AppointmentUnavailableException.class);
        assertThat(propagation.currentTransactionId()).isEmpty();
    }

    @Test
    void cancelRollsBackAndConcurrentHoldCannotTakeTheSameAppointment() {
        var hold = hold(first);
        assertError(post("/hold?appointmentId=" + first.id(), ""), 409, "BUSY");
        // A failed new transaction must not change the original hold's propagation state.
        var cancelled = post("/cancel", completion(hold));
        assertThat(cancelled.code()).isEqualTo(200);
        assertThat(cancelled.body()).containsEntry("outcome", "CANCELLED").containsEntry("databaseStatus", "AVAILABLE");
        assertThat(locks.lockNowait(first.id())).isEqualTo(first.id());
        assertError(post("/cancel", completion(hold)), 409, "UNAVAILABLE");
        var nextHold = hold(first);
        assertThat(nextHold.get("transactionId")).isNotEqualTo(hold.get("transactionId"));
        assertThat(post("/cancel", completion(nextHold)).code()).isEqualTo(200);
    }

    @Test
    void resetsAndBookingEndpointsRespectScenarioBoundariesAndPendingHolds() {
        var priorityFixture = appointments.save(new Appointment("Priority isolation test", 92).withStatus(BOOKED_FOR_REGULAR));
        try {
            var pending = hold(first);
            var confirmed = hold(second);
            assertThat(post("/confirm", completion(confirmed)).code()).isEqualTo(200);

            var reset = client.toBlocking().exchange(HttpRequest.POST(BASE + "/reset", "")
                    .accept(MediaType.APPLICATION_JSON_TYPE), Argument.listOf(Appointment.class));
            assertThat(reset.code()).isEqualTo(200);
            assertThat(appointments.findById(priorityFixture.id()).orElseThrow().status()).isEqualTo(BOOKED_FOR_REGULAR);
            assertThat(appointments.findById(second.id()).orElseThrow().status()).isEqualTo(AVAILABLE);
            priority.resetFixture();
            assertThat(post("/confirm", completion(pending)).code()).isEqualTo(200);
            priority.resetFixture();
            assertThat(appointments.findById(first.id()).orElseThrow().status()).isEqualTo(BOOKED_AFTER_CONFIRMATION);

            assertNotFound(post("/hold?appointmentId=" + priorityFixture.id(), ""), BASE + "/hold");
            var response = client.toBlocking().exchange(HttpRequest.POST(
                    "/oracle/transaction-priority/book?appointmentId=" + first.id() + "&type=emergency", "")
                    .accept(MediaType.APPLICATION_JSON_TYPE), Argument.of(Map.class), Argument.of(Map.class));
            assertThat(response.code()).isEqualTo(404);
            assertThatThrownBy(() -> service.hold(priorityFixture.id())).isInstanceOf(NotFoundEntityException.class);
        } finally {
            appointments.deleteById(priorityFixture.id());
        }
    }

    @Test
    void validatesIdsAndDoesNotCommitWhenTheTransactionBelongsToAnotherAppointment() {
        assertNotFound(post("/hold?appointmentId=2147483647", ""), BASE + "/hold");
        assertThat(post("/hold", "").code()).isEqualTo(400);
        assertThat(post("/hold?appointmentId=invalid", "").code()).isEqualTo(400);
        assertThat(post("/hold?appointmentId=0", "").code()).isEqualTo(400);
        assertThat(post("/confirm", Map.of("appointmentId", first.id())).code()).isEqualTo(400);
        assertThat(post("/cancel", Map.of("transactionId", "YWJj")).code()).isEqualTo(400);
        assertThat(post("/confirm", Map.of("appointmentId", first.id(), "transactionId", "x".repeat(87))).code()).isEqualTo(400);
        var malformed = client.toBlocking().exchange(HttpRequest.POST(BASE + "/confirm", "{")
                .contentType(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON_TYPE),
                Argument.of(Map.class), Argument.of(Map.class));
        assertThat(malformed.code()).isEqualTo(400);
        var invalidTransaction = post("/confirm", Map.of("appointmentId", first.id(), "transactionId", "!bad!"));
        assertThat(invalidTransaction.code()).isEqualTo(400);
        assertThat(invalidTransaction.body()).containsEntry("message", "The transaction ID is malformed.");
        assertError(post("/confirm", Map.of("appointmentId", first.id(), "transactionId", "bm90LXJlYWw")),
                409, "UNAVAILABLE");

        var hold = hold(first);
        assertError(post("/confirm", Map.of("appointmentId", second.id(), "transactionId", hold.get("transactionId"))),
                409, "UNAVAILABLE");
        assertThat(appointments.findById(first.id()).orElseThrow().status()).isEqualTo(AVAILABLE);
        assertThat(appointments.findById(second.id()).orElseThrow().status()).isEqualTo(AVAILABLE);
        assertThat(locks.lockNowait(first.id())).isEqualTo(first.id());
    }

    @Test
    void abandonedHoldExpiresAndReleasesItsLock() throws InterruptedException {
        var hold = hold(first);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(75);
        boolean released = false;
        while (System.nanoTime() < deadline) {
            try {
                locks.lockNowait(first.id());
                released = true;
                break;
            } catch (DataAccessException error) {
                assertThat(error.getCause()).isInstanceOf(SQLException.class);
                assertThat(((SQLException) error.getCause()).getErrorCode())
                        .isEqualTo(OracleErrorCode.ORA_00054_RESOURCE_BUSY.code());
            }
            Thread.sleep(250);
        }
        assertThat(released).as("Oracle releases the abandoned hold within 75 seconds").isTrue();
        assertThat(appointments.findById(first.id()).orElseThrow().status()).isEqualTo(AVAILABLE);
        assertError(post("/confirm", completion(hold)), 409, "UNAVAILABLE");
    }

    @Test
    void resumesOnANewPhysicalDatabaseSession() throws SQLException {
        String originalSession = databaseSession();
        var hold = hold(first);
        // The sole idle connection was used to create the hold. Retire it so completion
        // must use a different Oracle session, rather than merely another pool checkout.
        dataSourceResolver.resolve(dataSource).unwrap(HikariDataSource.class).getHikariPoolMXBean().softEvictConnections();
        assertThat(databaseSession()).isNotEqualTo(originalSession);
        assertThat(post("/confirm", completion(hold)).code()).isEqualTo(200);
        assertThat(appointments.findById(first.id()).orElseThrow().status()).isEqualTo(BOOKED_AFTER_CONFIRMATION);
    }

    private String databaseSession() throws SQLException {
        try (var connection = dataSourceResolver.resolve(dataSource).getConnection();
             var statement = connection.prepareStatement("SELECT SYS_CONTEXT('USERENV', 'SESSIONID') FROM DUAL");
             var result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            return result.getString(1);
        }
    }

    @Test
    void rendersBothExamplesAndLocalizesSessionlessControls() {
        for (var locale : List.of(Locale.ENGLISH, Locale.GERMAN, Locale.forLanguageTag("es"))) {
            var response = client.toBlocking().exchange(HttpRequest.GET("/oracle/transaction-priority")
                    .accept(MediaType.TEXT_HTML_TYPE).header("Accept-Language", locale.toLanguageTag()), String.class);
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.body()).contains("priority-start-low", "sessionless-hold", "sessionless-booking.js")
                    .contains(messages.getMessage("sessionless.title", locale).orElseThrow())
                    .doesNotContain("data-sessionless-key=\"error\">sessionless.");
        }
    }

    private Map<String, Object> hold(Appointment appointment) {
        var response = post("/hold?appointmentId=" + appointment.id(), "");
        assertThat(response.code()).isEqualTo(201);
        Map<String, Object> body = response.body();
        assertThat(body).containsEntry("outcome", "HELD");
        assertThat(body.get("transactionId")).isInstanceOf(String.class);
        holds.add(body);
        return body;
    }

    private static Map<String, Object> completion(Map<String, Object> hold) {
        return Map.of("appointmentId", hold.get("appointmentId"), "transactionId", hold.get("transactionId"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private HttpResponse<Map> post(String path, Object body) {
        return client.toBlocking().exchange(HttpRequest.POST(BASE + path, body).accept(MediaType.APPLICATION_JSON_TYPE),
                Argument.of(Map.class), Argument.of(Map.class));
    }

    private String getAppointments() {
        return client.toBlocking().retrieve(HttpRequest.GET(BASE + "/appointments").accept(MediaType.APPLICATION_JSON_TYPE));
    }

    private static void assertError(HttpResponse<Map> response, int status, String code) {
        assertThat(response.code()).as("%s", response.body()).isEqualTo(status);
        assertThat(response.body()).containsEntry("code", code).doesNotContainKey("transactionId");
    }

    private static void assertNotFound(HttpResponse<Map> response, String path) {
        assertThat(response.code()).as("%s", response.body()).isEqualTo(404);
        assertThat(response.body()).containsEntry("path", path).containsEntry("message", "Appointment not found.");
    }
}
