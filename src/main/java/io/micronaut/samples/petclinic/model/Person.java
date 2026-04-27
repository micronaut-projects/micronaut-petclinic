package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Base entity class representing a person.
 * Provides first name and last name properties.
 */
@MappedEntity
@Serdeable
public abstract class Person extends BaseEntity {

    @MappedProperty("first_name")
    @NotBlank
    private String firstName;

    @MappedProperty("last_name")
    @NotBlank
    private String lastName;

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Get the full name (first name + last name).
     * @return the person's full name
     */
    @io.micronaut.data.annotation.Transient
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
}
