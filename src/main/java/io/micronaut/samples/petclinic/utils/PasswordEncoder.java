package io.micronaut.samples.petclinic.utils;

import jakarta.validation.constraints.NotBlank;

/**
 * Encodes and verifies user passwords.
 */
public interface PasswordEncoder {

    /**
     * Encodes a raw password for storage.
     *
     * @param rawPassword the submitted password
     * @return an encoded password hash
     */
    String encode(@NotBlank String rawPassword);

    /**
     * Checks whether a raw password matches a stored hash.
     *
     * @param rawPassword the submitted password
     * @param encodedPassword the stored encoded password hash
     * @return {@code true} when the password matches
     */
    boolean matches(@NotBlank String rawPassword,
                    @NotBlank String encodedPassword);
}
