package io.micronaut.samples.petclinic.oracle;

import org.jspecify.annotations.Nullable;

/** Oracle JDBC error numbers that the application handles explicitly. */
public enum OracleErrorCode {
    /** Resource is busy; acquisition used NOWAIT or its timeout expired. */
    ORA_00054_RESOURCE_BUSY(54),
    /** The user requested cancellation of the current operation. */
    ORA_01013_USER_REQUESTED_CANCEL(1013),
    /** A deferred error from a pipelined sessionless-transaction call. */
    ORA_18745_SESSIONLESS_TRANSACTION_PIGGYBACKED_CALL_FAILED(18745),
    /** The requested transaction does not exist or has already completed. */
    ORA_24756_TRANSACTION_DOES_NOT_EXIST(24756),
    /** The transaction is currently attached to another session. */
    ORA_25351_TRANSACTION_ATTACHED_TO_OTHER_SESSION(25351),
    /** The requested sessionless transaction does not exist or has expired. */
    ORA_26218_SESSIONLESS_TRANSACTION_DOES_NOT_EXIST(26218),
    /** Resource acquisition with WAIT timed out. */
    ORA_30006_RESOURCE_BUSY_WAIT_TIMEOUT(30006),
    /** The duality-view table does not allow INSERT. */
    ORA_40937_DUALITY_VIEW_INSERT_NOT_ALLOWED(40937),
    /** The duality-view table does not allow DELETE. */
    ORA_40938_DUALITY_VIEW_DELETE_NOT_ALLOWED(40938),
    /** The duality-view table does not allow UPDATE. */
    ORA_40939_DUALITY_VIEW_UPDATE_NOT_ALLOWED(40939),
    /** The duality-view column does not allow UPDATE. */
    ORA_40940_DUALITY_VIEW_COLUMN_UPDATE_NOT_ALLOWED(40940),
    /** The duality-view column is not updatable. */
    ORA_42689_DUALITY_VIEW_COLUMN_NOT_UPDATABLE(42689);

    private final int code;

    OracleErrorCode(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric value exposed by {@link java.sql.SQLException#getErrorCode()}.
     *
     * @return Oracle error number
     */
    public int code() {
        return code;
    }

    /**
     * Finds a known Oracle error by its JDBC numeric value.
     *
     * @param code value returned by {@link java.sql.SQLException#getErrorCode()}
     * @return the matching enum value, or {@code null} when the code is not defined here
     */
    public static @Nullable OracleErrorCode fromCode(int code) {
        for (OracleErrorCode oracleErrorCode : values()) {
            if (oracleErrorCode.code == code) return oracleErrorCode;
        }
        return null;
    }
}
