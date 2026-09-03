package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.model.UserState;
import io.micronaut.samples.petclinic.repository.UserJdbcRepository;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Database-backed implementation that adapts {@link io.micronaut.samples.petclinic.model.User} to {@link UserState}.
 */
@Singleton
class UserFetcherService implements UserFetcher {

    private final UserJdbcRepository userJdbcRepository;

    /**
     * Creates the service.
     *
     * @param userJdbcRepository repository used to load users
     */
    UserFetcherService(UserJdbcRepository userJdbcRepository) {
        this.userJdbcRepository = userJdbcRepository;
    }

    /**
     * Finds a user's authentication state by user name.
     *
     * @param username the user name to search for
     * @return the user state, or empty when no user exists
     */
    @Override
    public Optional<UserState> findByUsername(@NotBlank @NonNull String username) {
        return userJdbcRepository.findByUsername(username).map(UserState.class::cast);
    }
}
