package io.micronaut.samples.petclinic.controller;

import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.samples.petclinic.exception.AppointmentUnavailableException;
import io.micronaut.samples.petclinic.oracle.OracleErrorCode;
import io.micronaut.transaction.exceptions.OracleTransactionPriorityException;
import io.micronaut.transaction.exceptions.TransactionTimedOutException;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;

import static io.micronaut.samples.petclinic.dto.OracleBookingResult.Outcome.*;
import static org.assertj.core.api.Assertions.assertThat;

class OracleTransactionPriorityOutcomeTest {
    @Test
    void recognizesMicronautPriorityRollbackException() {
        var failure = new OracleTransactionPriorityException("Priority rollback");
        assertThat(OracleTransactionPriorityController.outcomeOf(failure))
                .isEqualTo(PRIORITY_ROLLED_BACK);
        assertThat(OracleTransactionPriorityController.outcomeOf(new RuntimeException(failure)))
                .isEqualTo(FAILED);
    }

    @Test
    void lockTimeoutsAndOrdinaryFailuresAreNotPriorityRollbacks() {
        for (OracleErrorCode code : new OracleErrorCode[]{
                OracleErrorCode.ORA_00054_RESOURCE_BUSY,
                OracleErrorCode.ORA_30006_RESOURCE_BUSY_WAIT_TIMEOUT,
                OracleErrorCode.ORA_01013_USER_REQUESTED_CANCEL
        }) {
            var failure = new DataAccessException("Query failed",
                    new SQLException("lock timeout", "72000", code.code()));
            assertThat(OracleTransactionPriorityController.outcomeOf(failure))
                    .isEqualTo(TIMED_OUT);
        }
        assertThat(OracleTransactionPriorityController.outcomeOf(new TransactionTimedOutException("Expired")))
                .isEqualTo(TIMED_OUT);
        assertThat(OracleTransactionPriorityController.outcomeOf(
                new DataAccessException("Query failed", new SQLException("constraint violation", "72000", 1))))
                .isEqualTo(FAILED);
        assertThat(OracleTransactionPriorityController.outcomeOf(new AppointmentUnavailableException()))
                .isEqualTo(TAKEN);
        assertThat(OracleTransactionPriorityController.outcomeOf(new IllegalStateException("unexpected failure")))
                .isEqualTo(FAILED);
        assertThat(OracleTransactionPriorityController.outcomeOf(new IllegalStateException("ORA-63300")))
                .isEqualTo(FAILED);
    }
}
