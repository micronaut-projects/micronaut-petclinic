package io.micronaut.samples.petclinic.dto;

import io.micronaut.samples.petclinic.annotation.PasswordMatch;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

/**
 * Form payload used to create a new application user.
 *
 * @param username the requested user name, stored as an email address
 * @param password the requested raw password
 * @param repeatPassword the repeated raw password entered for confirmation
 */
@PasswordMatch
@Serdeable
public record SignUpForm(@NotBlank String username,
                         @NotBlank String password,
                         @NotBlank String repeatPassword) {
}
