package io.micronaut.samples.petclinic.service;

import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.samples.petclinic.exception.AppointmentUnavailableException.Reason;
import io.micronaut.samples.petclinic.oracle.OracleErrorCode;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class OracleSessionlessBookingOutcomeTest {
    @Test
    void recognizesCausesInsideTheJdbcNextExceptionChain() {
        for (OracleErrorCode code : new OracleErrorCode[]{
                OracleErrorCode.ORA_26218_SESSIONLESS_TRANSACTION_DOES_NOT_EXIST,
                OracleErrorCode.ORA_25351_TRANSACTION_ATTACHED_TO_OTHER_SESSION
        }) {
            var root = new SQLException("query wrapper", "72000", 0);
            root.setNextException(new SQLException("deferred resume", "72000",
                    OracleErrorCode.ORA_18745_SESSIONLESS_TRANSACTION_PIGGYBACKED_CALL_FAILED.code(),
                    new SQLException("original failure", "72000", code.code())));
            assertThat(OracleSessionlessBookingService.unavailabilityReason(new DataAccessException("query", root)))
                    .isEqualTo(code == OracleErrorCode.ORA_25351_TRANSACTION_ATTACHED_TO_OTHER_SESSION
                            ? Reason.BUSY : Reason.UNAVAILABLE);
        }
    }

    @Test
    void stopsWhenExceptionCausesFormACycle() {
        var first = new SQLException("first");
        var second = new SQLException("second", first);
        first.initCause(second);
        assertThat(OracleSessionlessBookingService.unavailabilityReason(new DataAccessException("query", first)))
                .isNull();
    }

    @Test
    void recognizesPipelinedResumeFailuresWithoutParsingMessages() {
        for (OracleErrorCode code : new OracleErrorCode[]{
                OracleErrorCode.ORA_26218_SESSIONLESS_TRANSACTION_DOES_NOT_EXIST,
                OracleErrorCode.ORA_24756_TRANSACTION_DOES_NOT_EXIST,
                OracleErrorCode.ORA_25351_TRANSACTION_ATTACHED_TO_OTHER_SESSION
        }) {
            var original = new SQLException("private transaction ID", "72000", code.code());
            var deferred = new SQLException("deferred resume", "72000",
                    OracleErrorCode.ORA_18745_SESSIONLESS_TRANSACTION_PIGGYBACKED_CALL_FAILED.code(), original);
            assertThat(OracleSessionlessBookingService.unavailabilityReason(new DataAccessException("query", deferred)))
                    .isEqualTo(code == OracleErrorCode.ORA_25351_TRANSACTION_ATTACHED_TO_OTHER_SESSION
                            ? Reason.BUSY : Reason.UNAVAILABLE);
        }
        var chain = new SQLException("deferred resume", "72000",
                OracleErrorCode.ORA_18745_SESSIONLESS_TRANSACTION_PIGGYBACKED_CALL_FAILED.code());
        chain.setNextException(new SQLException("private transaction ID", "72000",
                OracleErrorCode.ORA_26218_SESSIONLESS_TRANSACTION_DOES_NOT_EXIST.code()));
        assertThat(OracleSessionlessBookingService.unavailabilityReason(new DataAccessException("query", chain)))
                .isEqualTo(Reason.UNAVAILABLE);
        assertThat(OracleSessionlessBookingService.unavailabilityReason(new IllegalStateException("ORA-26218")))
                .isNull();
        assertThat(OracleSessionlessBookingService.unavailabilityReason(
                new DataAccessException("constraint", new SQLException("constraint", "72000", 1))))
                .isNull();
    }
}
