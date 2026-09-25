package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.Embeddable;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.annotation.sql.JoinColumn;

import java.util.Objects;

/**
 * Composite identifier for a {@link UserRole} assignment.
 */
@Embeddable
public class UserRoleId {

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    @JoinColumn(name = "user_id")
    private final User user;

    @Relation(value = Relation.Kind.MANY_TO_ONE)
    @JoinColumn(name = "role_id")
    private final Role role;

    /**
     * Creates a composite id for a user-role assignment.
     *
     * @param user the assigned user
     * @param role the assigned role
     */
    public UserRoleId(User user, Role role) {
        this.user = user;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserRoleId userRoleId = (UserRoleId) o;
        return role.id().equals(userRoleId.getRole().id()) &&
                user.id().equals(userRoleId.getUser().id());
    }

    @Override
    public int hashCode() {
        return Objects.hash(role.id(), user.id());
    }

    /**
     * @return the assigned user
     */
    public User getUser() {
        return user;
    }

    /**
     * @return the assigned role
     */
    public Role getRole() {
        return role;
    }
}
