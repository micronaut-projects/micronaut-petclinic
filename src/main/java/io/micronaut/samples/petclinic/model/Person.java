package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;

/**
 * Base entity class representing a person.
 * Provides first name and last name properties.
 */
@MappedSuperclass
@Serdeable
public abstract class Person extends BaseEntity {

    @Column(name = "first_name")
    @NotBlank
    private String firstName;

    @Column(name = "last_name")
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
    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }
}
