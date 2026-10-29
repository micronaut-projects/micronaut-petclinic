package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;

/** Raised only when Oracle rejects the points nonnegative constraint. */
public final class InsufficientLoyaltyPointsException extends IllegalRequestArgumentException {

    /** Creates the domain error; the HTTP handler supplies its localized message. */
    public InsufficientLoyaltyPointsException() {
        super("The owner does not have enough loyalty points for this reward.");
    }
}
