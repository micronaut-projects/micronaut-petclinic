package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.UserRole;
import io.micronaut.samples.petclinic.model.UserRoleId;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Repository for user-role assignment records.
 */
public interface UserRoleJdbcRepository extends CrudRepository<UserRole, UserRoleId> {

    /**
     * Finds all authority names granted to a user.
     *
     * @param username the username whose authorities should be loaded
     * @return authority names such as {@code ROLE_ADMIN}
     */
    List<String> findAllAuthoritiesByUsername(@NotBlank String username);
}
