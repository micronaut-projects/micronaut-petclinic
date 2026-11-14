package io.micronaut.samples.petclinic.security;

import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the custom authentication provider.
 */
@MicronautTest
class DelegatingAuthenticationProviderTest {

    @Inject
    DelegatingAuthenticationProvider<Object> authenticationProvider;

    @Test
    void shouldAuthenticateKnownUserAndReturnAuthorities() {
        AuthenticationResponse response = authenticate("admin@example.com", "password123");

        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getAuthentication()).isPresent();
        assertThat(response.getAuthentication().orElseThrow().getName()).isEqualTo("admin@example.com");
        assertThat(response.getAuthentication().orElseThrow().getRoles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldRejectInvalidPassword() {
        AuthenticationResponse response = authenticate("admin@example.com", "bad-password");

        assertThat(response.isAuthenticated()).isFalse();
        assertThat(response).isInstanceOf(AuthenticationFailed.class);
        assertThat(((AuthenticationFailed) response).getReason())
                .isEqualTo(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH);
    }

    @Test
    void shouldRejectUnknownUser() {
        AuthenticationResponse response = authenticate("missing@example.com", "password123");

        assertThat(response.isAuthenticated()).isFalse();
        assertThat(response).isInstanceOf(AuthenticationFailed.class);
        assertThat(((AuthenticationFailed) response).getReason())
                .isEqualTo(AuthenticationFailureReason.USER_NOT_FOUND);
    }

    private AuthenticationResponse authenticate(String username, String password) {
        return authenticationProvider.authenticate(
                null,
                new UsernamePasswordCredentials(username, password)
        );
    }
}
