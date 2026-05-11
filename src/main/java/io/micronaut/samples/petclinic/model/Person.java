package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.Transient;

/**
 * Shared contract for people in the domain model.
 */
public sealed interface Person extends BaseEntity permits Owner, Vet {

    /**
     * Returns the person's given name.
     *
     * @return the first name
     */
    String firstName();

    /**
     * Returns the person's family name.
     *
     * @return the last name
     */
    String lastName();

    /**
     * JavaBean-style alias for {@link #firstName()} used by views and framework code.
     *
     * @return the first name
     */
    default String getFirstName() {
        return firstName();
    }

    /**
     * JavaBean-style alias for {@link #lastName()} used by views and framework code.
     *
     * @return the last name
     */
    default String getLastName() {
        return lastName();
    }

    /**
     * Returns a display name composed from the first and last names.
     *
     * @return the full name
     */
    @Transient
    default String getFullName() {
        return firstName() + " " + lastName();
    }
}
