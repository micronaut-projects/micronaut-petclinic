package io.micronaut.samples.petclinic.constraint;

import io.micronaut.samples.petclinic.dto.SignUpForm;
import io.micronaut.samples.petclinic.dto.VisitForm;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for application custom validation constraints.
 */
@MicronautTest
class CustomValidatorFactoryTest {

    @Inject
    Validator validator;

    @Test
    void shouldAllowFutureVisitDate() {
        Set<ConstraintViolation<VisitForm>> violations = validator.validate(
                new VisitForm(LocalDate.now().plusDays(1), "Follow-up", 30, 6)
        );

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectVisitDateThatIsNotInTheFuture() {
        Set<ConstraintViolation<VisitForm>> violations = validator.validate(
                new VisitForm(LocalDate.now().minusDays(7), "Follow-up", 30, 3)
        );

        assertThat(violations)
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("date");
                    assertThat(violation.getMessage()).isEqualTo("Visit date must be in the future");
                });
    }

    @Test
    void shouldAllowMatchingSignupPasswords() {
        Set<ConstraintViolation<SignUpForm>> violations = validator.validate(
                new SignUpForm("new-user@example.com", "password123", "password123")
        );

        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectMismatchedSignupPasswords() {
        Set<ConstraintViolation<SignUpForm>> violations = validator.validate(
                new SignUpForm("new-user@example.com", "password123", "different123")
        );

        assertThat(violations)
                .singleElement()
                .satisfies(violation -> assertThat(violation.getMessage()).isEqualTo("Passwords must match"));
    }
}
