package io.micronaut.samples.petclinic.controller;

import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.samples.petclinic.service.OracleTransactionPriorityService;
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
        for (int code : new int[]{54, 30006, 1013}) {
            var failure = new DataAccessException("Query failed", new SQLException("lock timeout", "72000", code));
            assertThat(OracleTransactionPriorityController.outcomeOf(failure))
                    .isEqualTo(TIMED_OUT);
        }
        assertThat(OracleTransactionPriorityController.outcomeOf(new TransactionTimedOutException("Expired")))
                .isEqualTo(TIMED_OUT);
        assertThat(OracleTransactionPriorityController.outcomeOf(
                new DataAccessException("Query failed", new SQLException("constraint violation", "72000", 1))))
                .isEqualTo(FAILED);
        assertThat(OracleTransactionPriorityController.outcomeOf(new OracleTransactionPriorityService.BookingTaken()))
                .isEqualTo(TAKEN);
        assertThat(OracleTransactionPriorityController.outcomeOf(new IllegalStateException("unexpected failure")))
                .isEqualTo(FAILED);
        assertThat(OracleTransactionPriorityController.outcomeOf(new IllegalStateException("ORA-63300")))
                .isEqualTo(FAILED);
    }
}
