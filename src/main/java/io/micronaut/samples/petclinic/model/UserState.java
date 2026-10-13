package io.micronaut.samples.petclinic.model;

/**
 * Minimal account state needed by the authentication provider.
 */
public sealed interface UserState permits User {
    /**
     * @return the login name
     */
    String username();

    /**
     * @return the encoded password hash
     */
    String password();

    /**
     * @return whether the user is allowed to authenticate
     */
    boolean enabled();

    /**
     * @return whether the account has expired
     */
    boolean expired();

    /**
     * @return whether the account is locked
     */
    boolean locked();

    /**
     * @return whether the password has expired
     */
    boolean passwordExpired();
}
