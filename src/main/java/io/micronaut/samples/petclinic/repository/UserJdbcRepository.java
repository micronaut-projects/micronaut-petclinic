package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.User;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Repository for application users.
 */
public interface UserJdbcRepository extends CrudRepository<User, Long> {

    /**
     * Finds a user by login name.
     *
     * @param username the user name to search for
     * @return the matching user, or empty when no user exists
     */
    Optional<User> findByUsername(@NonNull @NotBlank String username);
}
