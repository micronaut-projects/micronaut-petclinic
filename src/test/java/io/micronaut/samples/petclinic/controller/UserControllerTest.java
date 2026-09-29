package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.samples.petclinic.model.User;
import io.micronaut.samples.petclinic.repository.UserJdbcRepository;
import io.micronaut.samples.petclinic.utils.PasswordEncoder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.createNoRedirectClient;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.exchange;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.formPost;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.login;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.loginResponse;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for user authentication and registration endpoints.
 */
@MicronautTest
class UserControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    EmbeddedServer server;

    @Inject
    UserJdbcRepository userJdbcRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    @Test
    void shouldRenderLoginPage() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/user/auth"), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldShowLoginLinkForAnonymousUsers() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/"), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("href=\"/user/auth\"");
        assertThat(response.body()).contains("Log in");
        assertThat(response.body()).doesNotContain("action=\"/logout\"");
    }

    @Test
    void shouldRenderSignUpPage() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/user/signUp"), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Create account");
    }

    @Test
    void shouldRedirectToAuthFailedWhenLoginFails() {
        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            HttpResponse<String> response = loginResponse(noRedirectClient, "admin@example.com", "wrong-password");

            assertThat(response.status().getCode()).isBetween(300, 399);
            assertThat(response.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo("/user/authFailed");
        }
    }

    @Test
    void shouldLoginAndUseSessionCookieForProtectedPages() {
        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            String sessionCookie = login(noRedirectClient, "admin@example.com", "password123");
            assertThat(sessionCookie).isNotBlank();

            HttpResponse<String> protectedResponse = exchange(noRedirectClient, HttpRequest.GET("/owners/1/pets/1/edit")
                    .header(HttpHeaders.COOKIE, sessionCookie));

            assertThat((CharSequence) protectedResponse.status()).isEqualTo(HttpStatus.OK);
            assertThat(protectedResponse.body()).contains("Leo");
        }
    }

    @Test
    void shouldShowLogoutButtonForAuthenticatedUsers() {
        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            String sessionCookie = login(noRedirectClient, "admin@example.com", "password123");

            HttpResponse<String> response = exchange(noRedirectClient, HttpRequest.GET("/")
                    .header(HttpHeaders.COOKIE, sessionCookie));

            assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
            assertThat(response.body()).contains("action=\"/logout\"");
            assertThat(response.body()).contains("Log out");
            assertThat(response.body()).doesNotContain("href=\"/user/auth\"");
        }
    }

    @Test
    void shouldLogoutAuthenticatedSession() {
        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            String sessionCookie = login(noRedirectClient, "admin@example.com", "password123");

            HttpResponse<String> logoutResponse = exchange(noRedirectClient, formPost("/logout", Map.of())
                    .header(HttpHeaders.COOKIE, sessionCookie));

            assertThat(logoutResponse.status().getCode()).isBetween(300, 399);
            assertThat(logoutResponse.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo("/");

            HttpResponse<String> protectedResponse = exchange(noRedirectClient, HttpRequest.GET("/owners/1/pets/1/edit")
                    .header(HttpHeaders.COOKIE, sessionCookie));

            assertThat((CharSequence) protectedResponse.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void shouldRedirectAnonymousUserFromAuthenticatedPageToLogin() {
        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            HttpResponse<String> response = exchange(noRedirectClient, HttpRequest.GET("/owners/1/pets/1/edit")
                    .accept(MediaType.TEXT_HTML));

            assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.SEE_OTHER);
            assertThat(response.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo("/user/auth");
        }
    }

    @Test
    void shouldRegisterNewUserWithEncodedPasswordAndRedirectToLogin() {
        String username = "user-" + UUID.randomUUID() + "@example.com";
        String password = "password123";

        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            HttpResponse<String> response = exchange(noRedirectClient, formPost("/user/signUp", Map.of(
                    "username", username,
                    "password", password,
                    "repeatPassword", password
            )));

            assertThat(response.status().getCode()).isBetween(300, 399);
            assertThat(response.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo("/user/auth");
        }

        Optional<User> user = userJdbcRepository.findByUsername(username);
        assertThat(user).isPresent();
        assertThat(user.get().password()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, user.get().password())).isTrue();
    }

    @Test
    void shouldRejectDuplicateSignUp() {
        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            HttpResponse<String> response = exchange(noRedirectClient, formPost("/user/signUp", Map.of(
                    "username", "admin@example.com",
                    "password", "password123",
                    "repeatPassword", "password123"
            )));

            assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
