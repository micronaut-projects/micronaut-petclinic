package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Role;

import java.util.Optional;

/**
 * Repository for role records.
 */
public interface RoleJdbcRepository extends CrudRepository<Role, Long> {

    /**
     * Creates a role for the supplied authority.
     *
     * @param authority the authority to persist
     * @return the saved role
     */
    Role save(Role.Authority authority);

    /**
     * Finds a role by authority.
     *
     * @param authority the authority to search for
     * @return the matching role, or empty when no role exists
     */
    Optional<Role> findByAuthority(Role.Authority authority);
}
