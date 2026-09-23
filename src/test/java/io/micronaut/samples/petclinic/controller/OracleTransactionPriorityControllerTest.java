package io.micronaut.samples.petclinic.controller;

import io.micronaut.samples.petclinic.service.OracleTransactionPriorityWorker;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;

import static io.micronaut.samples.petclinic.dto.OracleBookingResult.Outcome.*;
import static org.assertj.core.api.Assertions.assertThat;

class OracleTransactionPriorityControllerTest {
    @Test
    void recognizesOracleRollbackThroughWrappedAndChainedExceptions() {
        var wrapper = new SQLException("wrapper");
        wrapper.setNextException(new SQLException("priority rollback", "72000", 63300));
        assertThat(OracleTransactionPriorityController.outcomeOf(new RuntimeException(wrapper)))
                .isEqualTo(PRIORITY_ROLLED_BACK);
        assertThat(OracleTransactionPriorityController.outcomeOf(new SQLException("acknowledge rollback", "72000", 63302)))
                .isEqualTo(PRIORITY_ROLLED_BACK);
    }

    @Test
    void lockTimeoutsAndOrdinaryFailuresAreNotPriorityRollbacks() {
        for (int code : new int[]{54, 30006, 1013}) {
            assertThat(OracleTransactionPriorityController.outcomeOf(new SQLException("lock timeout", "72000", code)))
                    .isEqualTo(TIMED_OUT);
        }
        assertThat(OracleTransactionPriorityController.outcomeOf(new OracleTransactionPriorityWorker.BookingTaken()))
                .isEqualTo(TAKEN);
        assertThat(OracleTransactionPriorityController.outcomeOf(new IllegalStateException("unexpected failure")))
                .isEqualTo(FAILED);
    }
}
