package io.micronaut.samples.petclinic.execption;

/**
 * Signals that registration cannot continue because a user already exists for the requested user name.
 */
public class UserAlreadyExistsException extends RuntimeException {
}
