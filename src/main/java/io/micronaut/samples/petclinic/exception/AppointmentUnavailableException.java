package io.micronaut.samples.petclinic.exception;

/** Signals that an appointment or its suspended transaction cannot currently be used.
 *
 * <p>The reason distinguishes a completed or expired hold from a transaction that is
 * currently busy.</p>
 */
public class AppointmentUnavailableException extends IllegalRequestArgumentException {

    /** Why the appointment or suspended transaction could not be used. */
    public enum Reason {
        /** The requested appointment or transaction is no longer available, such as when booked or completed. */
        UNAVAILABLE,
        /** The operation could not proceed because the appointment or transaction is busy or timed out. */
        BUSY
    }

    private final Reason reason;

    /** Creates an exception for an appointment or transaction that is unavailable. */
    public AppointmentUnavailableException() {
        this(Reason.UNAVAILABLE, null);
    }

    /**
     * Creates an exception with a specific reason and optional database failure.
     *
     * @param reason whether the resource is unavailable or busy
     * @param cause underlying failure, if Oracle reported one
     */
    public AppointmentUnavailableException(Reason reason, Throwable cause) {
        super("The appointment or transaction is unavailable.", cause);
        this.reason = reason;
    }

    /**
     * Returns the reason this appointment or transaction could not be used.
     *
     * @return the availability reason
     */
    public Reason getReason() {
        return reason;
    }
}
