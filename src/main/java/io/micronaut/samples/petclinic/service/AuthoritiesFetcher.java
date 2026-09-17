package io.micronaut.samples.petclinic.service;

import java.util.List;

/**
 * Loads authority names for an authenticated user.
 */
public interface AuthoritiesFetcher {

    /**
     * Finds all role authorities granted to the supplied user.
     *
     * @param username the user name whose authorities should be loaded
     * @return authority names used by Micronaut Security
     */
    List<String> findAuthoritiesByUsername(String username);
}
