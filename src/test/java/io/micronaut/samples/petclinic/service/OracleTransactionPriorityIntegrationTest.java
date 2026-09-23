package io.micronaut.samples.petclinic.service;

import com.zaxxer.hikari.HikariDataSource;
import io.micronaut.context.annotation.Property;
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

import static io.micronaut.samples.petclinic.model.Appointment.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Uses the configured Oracle database and an existing schema; only test-created rows are changed. */
@MicronautTest(environments = "oracle", transactional = false, startApplication = false, rebuildContext = true)
@Property(name = "datasources.default.schema-generate", value = "NONE")
@Property(name = "petclinic.sample-data.enabled", value = "false")
@Property(name = "petclinic.transaction-priority.reservation-seconds", value = "5")
class OracleTransactionPriorityIntegrationTest {
    @Inject OracleTransactionPriorityWorker worker;
    @Inject AppointmentRepository appointments;
    @Inject DataSource dataSource;

    private Integer appointmentId;

    @BeforeEach
    void createAppointment() {
        appointmentId = appointments.save(new Appointment("Priority test appointment", 99)).id();
    }

    @AfterEach
    void deleteAppointment() {
        if (appointmentId != null) appointments.deleteById(appointmentId);
    }

    @Test
    @Property(name = "petclinic.transaction-priority.reservation-seconds", value = "0")
    void regularBookingCommitsWithoutAnEmergency() {
        assertThat(worker.getReservationSeconds()).isZero();
        worker.bookRegular(appointmentId);

        assertThat(appointments.findById(appointmentId).orElseThrow().status()).isEqualTo(BOOKED_FOR_REGULAR);
    }

    @Test
    void emergencyCommitsAndOracleRollsBackRegularBooking() throws Exception {
        assertThat(worker.getReservationSeconds()).isEqualTo(5); // Exceeds Oracle's 3-second HIGH wait target.
        // Closing the executor waits for LOW before @AfterEach removes the appointment.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var regular = executor.submit(() -> worker.bookRegular(appointmentId));
            awaitRegularLock();

            worker.bookEmergency(appointmentId);
            assertThat(regular.isDone()).as("HIGH commits while LOW is still paused").isFalse();

            Throwable failure = assertThrows(ExecutionException.class, () -> regular.get(30, TimeUnit.SECONDS));
            while (!(failure instanceof SQLException) && failure.getCause() != null) {
                failure = failure.getCause();
            }
            assertThat(failure).isInstanceOfSatisfying(SQLException.class,
                    sql -> assertThat(sql.getErrorCode()).isIn(63300, 63302));
            assertThat(appointments.findById(appointmentId).orElseThrow().status()).isEqualTo(BOOKED_FOR_EMERGENCY);
        }
    }

    /** Wait for the actual row lock so HIGH cannot accidentally arrive before LOW. */
    private void awaitRegularLock() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        try (var connection = dataSource.unwrap(HikariDataSource.class).getConnection();
             var statement = connection.prepareStatement("SELECT ID FROM APPOINTMENTS WHERE ID = ? FOR UPDATE NOWAIT")) {
            connection.setAutoCommit(false);
            statement.setInt(1, appointmentId);
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
