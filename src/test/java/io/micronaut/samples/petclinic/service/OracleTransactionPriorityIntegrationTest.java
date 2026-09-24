package io.micronaut.samples.petclinic.service;

import com.zaxxer.hikari.HikariDataSource;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.micronaut.samples.petclinic.model.Appointment.Status.BOOKED_FOR_EMERGENCY;
import static io.micronaut.samples.petclinic.model.Appointment.Status.BOOKED_FOR_REGULAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Uses the existing schema and sample data without running seeders.
 * Each test reuses an available appointment and restores it afterward.
 * Keep the demo idle during this test; other sample data, including visits, is untouched.
 */
@MicronautTest
@Requires(env = "oracle")
@Property(name = "petclinic.transaction-priority.reservation-seconds", value = "0")
class OracleTransactionPriorityIntegrationTest {
    @Inject OracleTransactionPriorityService service;
    @Inject AppointmentRepository appointments;
    @Inject DataSource dataSource;

    private Appointment originalAppointment;

    @BeforeEach
    void useExistingAppointment() {
        originalAppointment = appointments.findAvailableAppointments().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "An available sample appointment is required; this test does not seed or reset the database."));
    }

    @AfterEach
    void restoreAppointment() {
        if (originalAppointment != null) appointments.save(originalAppointment);
    }

    @Test
    void regularBookingCommitsWithoutAnEmergency() {
        assertThat(service.getReservationSeconds()).isZero();
        service.bookRegular(originalAppointment.id());
        assertThat(appointments.findById(originalAppointment.id()).orElseThrow().status()).isEqualTo(BOOKED_FOR_REGULAR);
    }

    @Test
    @Property(name = "petclinic.transaction-priority.reservation-seconds", value = "5")
    void emergencyCommitsAndOracleRollsBackRegularBooking() throws Exception {
        assertThat(service.getReservationSeconds()).isEqualTo(5); // Exceeds Oracle's 3-second HIGH wait target.
        // Closing the executor waits for LOW before @AfterEach restores the appointment.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var regular = executor.submit(() -> service.bookRegular(originalAppointment.id()));
            awaitRegularLock();
            service.bookEmergency(originalAppointment.id());
            assertThat(regular.isDone()).as("HIGH commits while LOW is still paused").isFalse();
            Throwable failure = assertThrows(ExecutionException.class, () -> regular.get(30, TimeUnit.SECONDS));
            while (!(failure instanceof SQLException) && failure.getCause() != null) {
                failure = failure.getCause();
            }
            assertThat(failure).isInstanceOfSatisfying(SQLException.class,
                    sql -> assertThat(sql.getErrorCode()).isIn(63300, 63302));
            assertThat(appointments.findById(originalAppointment.id()).orElseThrow().status()).isEqualTo(BOOKED_FOR_EMERGENCY);
        }
    }

    /** Wait for the actual row lock so HIGH cannot accidentally arrive before LOW. */
    private void awaitRegularLock() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        try (var connection = dataSource.unwrap(HikariDataSource.class).getConnection();
             var statement = connection.prepareStatement("SELECT ID FROM APPOINTMENTS WHERE ID = ? FOR UPDATE NOWAIT")) {
            connection.setAutoCommit(false);
            statement.setInt(1, originalAppointment.id());
            while (System.nanoTime() < deadline) {
                try (var rows = statement.executeQuery()) {
                    assertThat(rows.next()).isTrue();
                } catch (SQLException error) {
                    if (error.getErrorCode() == 54) return; // LOW holds the lock.
                    throw error;
                } finally {
                    connection.rollback();
                }
                Thread.sleep(25);
            }
        }
        throw new AssertionError("LOW did not acquire the appointment lock within five seconds");
    }
}
