package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.repository.UserRoleJdbcRepository;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * Database-backed implementation that loads user authorities from user-role assignments.
 */
@Singleton
class AuthoritiesFetcherService implements AuthoritiesFetcher {

    private final UserRoleJdbcRepository userRoleJdbcRepository;

    /**
     * Creates the service.
     *
     * @param userRoleJdbcRepository repository used to query assigned roles
     */
    AuthoritiesFetcherService(UserRoleJdbcRepository userRoleJdbcRepository) {
        this.userRoleJdbcRepository = userRoleJdbcRepository;
    }

    /**
     * Finds all authorities for a user.
     *
     * @param username the user name whose authorities should be loaded
     * @return authority names granted to the user
     */
    @Override
    public List<String> findAuthoritiesByUsername(String username) {
        return userRoleJdbcRepository.findAllAuthoritiesByUsername(username);
    }
}
