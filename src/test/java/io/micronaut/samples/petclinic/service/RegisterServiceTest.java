package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.execption.UserAlreadyExistsException;
import io.micronaut.samples.petclinic.model.Role;
import io.micronaut.samples.petclinic.model.User;
import io.micronaut.samples.petclinic.repository.UserJdbcRepository;
import io.micronaut.samples.petclinic.repository.UserRoleJdbcRepository;
import io.micronaut.samples.petclinic.utils.PasswordEncoder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for user registration and role assignment.
 */
@MicronautTest
class RegisterServiceTest {

    @Inject
    RegisterService registerService;

    @Inject
    UserJdbcRepository userJdbcRepository;

    @Inject
    UserRoleJdbcRepository userRoleJdbcRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    @Test
    void shouldRegisterEnabledUserWithEncodedPassword() {
        String username = uniqueEmail();
        String password = "password123";

        registerService.register(username, password);

        Optional<User> user = userJdbcRepository.findByUsername(username);
        assertThat(user).isPresent();
        assertThat(user.get().enabled()).isTrue();
        assertThat(user.get().expired()).isFalse();
        assertThat(user.get().locked()).isFalse();
        assertThat(user.get().passwordExpired()).isFalse();
        assertThat(user.get().password()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, user.get().password())).isTrue();
    }

    @Test
    void shouldAssignRolesToRegisteredUser() {
        String username = uniqueEmail();

        registerService.register(username, "password123", List.of(
                Role.Authority.ROLE_ADMIN,
                Role.Authority.ROLE_STAFF
        ));

        assertThat(userRoleJdbcRepository.findAllAuthoritiesByUsername(username))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_STAFF");
    }

    @Test
    void shouldRejectDuplicateUsername() {
        String username = uniqueEmail();

        registerService.register(username, "password123");

        assertThatThrownBy(() -> registerService.register(username, "password123"))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    private static String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }
}
