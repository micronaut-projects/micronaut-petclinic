package io.micronaut.samples.petclinic.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level validation annotation for signup payloads whose password fields must contain
 * the same value.
 * <p>
 * The concrete validation logic is provided by the {@code ConstraintValidator} bean declared
 * in {@code CustomValidatorFactory}.
 */
@Constraint(validatedBy = {})
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatch {

    /**
     * @return the validation error message used when the password fields do not match
     */
    String message() default "Passwords must match";

    /**
     * @return validation groups that this constraint belongs to
     */
    Class<?>[] groups() default {};

    /**
     * @return payload types that clients can associate with this constraint
     */
    Class<? extends Payload>[] payload() default {};
}
