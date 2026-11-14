package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.model.UserState;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Loads the account state used during authentication.
 */
public interface UserFetcher {

    /**
     * Finds a user's authentication state by login name.
     *
     * @param username the user name to search for
     * @return the user state, or empty when the user does not exist
     */
    Optional<UserState> findByUsername(@NotBlank @NonNull String username);
}
