package io.micronaut.samples.petclinic.support;

import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.samples.petclinic.repository.OracleAppointmentLockRepository;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public final class OracleTransactionPriorityTestSupport {
    private OracleTransactionPriorityTestSupport() {
    }

    /** Wait for the actual row lock so HIGH cannot accidentally arrive before LOW. */
    public static void awaitRegularLock(OracleAppointmentLockRepository locks, Integer appointmentId)
            throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            try {
                assertThat(locks.lockNowait(appointmentId)).isEqualTo(appointmentId);
            } catch (DataAccessException error) {
                for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                    if (cause instanceof SQLException sql && sql.getErrorCode() == 54) return; // LOW holds the lock.
                }
                throw error;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("LOW did not acquire the appointment lock within five seconds");
    }
}
