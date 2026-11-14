package io.micronaut.samples.petclinic.utils;

import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt implementation of the application password encoder.
 */
@Singleton
class BCryptPasswordEncoderService implements PasswordEncoder {

    private final org.springframework.security.crypto.password.PasswordEncoder delegate = new BCryptPasswordEncoder();

    /**
     * Encodes a raw password with BCrypt.
     *
     * @param rawPassword the submitted password
     * @return the BCrypt password hash
     */
    @Override
    public String encode(@NotBlank @NonNull String rawPassword) {
        return delegate.encode(rawPassword);
    }

    /**
     * Checks a raw password against a BCrypt hash.
     *
     * @param rawPassword the submitted password
     * @param encodedPassword the stored BCrypt hash
     * @return {@code true} when the password matches
     */
    @Override
    public boolean matches(@NotBlank @NonNull String rawPassword,
                           @NotBlank @NonNull String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}
