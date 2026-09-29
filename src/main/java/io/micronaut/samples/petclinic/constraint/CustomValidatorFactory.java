package io.micronaut.samples.petclinic.constraint;

import io.micronaut.context.annotation.Factory;
import io.micronaut.samples.petclinic.annotation.PasswordMatch;
import io.micronaut.samples.petclinic.dto.SignUpForm;
import io.micronaut.validation.validator.constraints.ConstraintValidator;
import jakarta.inject.Singleton;

import java.time.LocalDate;

/**
 * Declares custom validation beans used by the application.
 * <p>
 * Micronaut resolves the validators produced by this factory and applies them to the
 * corresponding constraint annotations.
 */
@Factory
public class CustomValidatorFactory {

    /**
     * Creates the validator backing {@link PasswordMatch} for {@link SignUpForm}.
     * <p>
     * A {@code null} form, or a form where both password fields are {@code null}, is treated
     * as valid so that field-level constraints can report missing values independently. If only
     * one field is present, validation fails. Otherwise both values must be equal.
     *
     * @return a singleton validator for password confirmation checks
     */
    @Singleton
    ConstraintValidator<PasswordMatch, SignUpForm> passwordMatchValidator() {
        return (value, annotationMetadata, context) -> {
            if (value == null) {
                return true;
            }
            if (value.password() == null && value.repeatPassword() == null) {
                return true;
            }
            if (value.password() != null && value.repeatPassword() == null) {
                return false;
            }
            if (value.password() == null) {
                return false;
            }
            return value.password().equals(value.repeatPassword());
        };
    }

}
