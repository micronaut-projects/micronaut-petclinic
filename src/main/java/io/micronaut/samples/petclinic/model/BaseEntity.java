package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.Transient;

import java.util.Objects;

/**
 * Shared contract for persistent entities.
 *
 * Entity identity is represented by the nullable persistence identifier. New
 * instances have no identifier until a repository saves them and returns the
 * persisted copy.
 */
public sealed interface BaseEntity permits NamedEntity, Person, User, Visit, Role {

    /**
     * Returns the persistence identifier.
     *
     * @return the entity id, or {@code null} before the entity has been saved
     */
    Integer id();

    /**
     * Determines whether this entity has not yet been persisted.
     *
     * @return {@code true} when the entity has no id
     */
    @Transient
    default boolean isNew() {
        return id() == null;
    }

    /**
     * Compares two entities by concrete type and persistence identifier.
     *
     * @param entity the entity providing the equality implementation
     * @param other the object being compared
     * @return {@code true} when both objects have the same type and id
     */
    static boolean entityEquals(BaseEntity entity, Object other) {
        if (entity == other) {
            return true;
        }
        if (other == null || entity.getClass() != other.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) other;
        return Objects.equals(entity.id(), that.id());
    }

    /**
     * Computes a hash code based on the persistence identifier.
     *
     * @param entity the entity providing the hash-code implementation
     * @return a hash code for the entity id
     */
    static int entityHashCode(BaseEntity entity) {
        return Objects.hash(entity.id());
    }
}
