package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

/**
 * Form payload used by the login page.
 *
 * @param username the submitted user name, stored as an email address in this application
 * @param password the submitted raw password
 */
@Serdeable
public record LoginForm(@NotBlank String username,
                        @NotBlank String password) {
}
