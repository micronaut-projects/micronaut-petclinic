package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.EmbeddedId;
import io.micronaut.data.annotation.MappedEntity;

/**
 * Join entity that assigns one role to one user.
 */
@MappedEntity("USER_ROLE")
public class UserRole {

    @EmbeddedId
    private final UserRoleId id;

    /**
     * Creates a user-role assignment.
     *
     * @param id the composite key containing the user and role
     */
    public UserRole(UserRoleId id) {
        this.id = id;
    }

    /**
     * @return the composite user-role id
     */
    public UserRoleId getId() {
        return id;
    }
}
