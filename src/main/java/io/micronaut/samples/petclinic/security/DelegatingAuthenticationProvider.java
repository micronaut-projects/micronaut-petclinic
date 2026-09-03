package io.micronaut.samples.petclinic.security;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.samples.petclinic.model.UserState;
import io.micronaut.samples.petclinic.service.AuthoritiesFetcher;
import io.micronaut.samples.petclinic.service.UserFetcher;
import io.micronaut.samples.petclinic.utils.PasswordEncoder;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Singleton;

import java.util.List;

import static io.micronaut.security.authentication.AuthenticationFailureReason.ACCOUNT_EXPIRED;
import static io.micronaut.security.authentication.AuthenticationFailureReason.ACCOUNT_LOCKED;
import static io.micronaut.security.authentication.AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH;
import static io.micronaut.security.authentication.AuthenticationFailureReason.PASSWORD_EXPIRED;
import static io.micronaut.security.authentication.AuthenticationFailureReason.USER_DISABLED;
import static io.micronaut.security.authentication.AuthenticationFailureReason.USER_NOT_FOUND;

/**
 * Micronaut Security authentication provider backed by the application's user tables.
 *
 * @param <B> the HTTP request body type
 */
@Singleton
class DelegatingAuthenticationProvider<B> implements HttpRequestAuthenticationProvider<B> {

    private final UserFetcher userFetcher;
    private final PasswordEncoder passwordEncoder;
    private final AuthoritiesFetcher authoritiesFetcher;

    /**
     * Creates the provider with services used to load and verify users.
     *
     * @param userFetcher loads persisted user state
     * @param passwordEncoder verifies submitted passwords
     * @param authoritiesFetcher loads user authorities after successful authentication
     */
    DelegatingAuthenticationProvider(UserFetcher userFetcher,
                                     PasswordEncoder passwordEncoder,
                                     AuthoritiesFetcher authoritiesFetcher) {
        this.userFetcher = userFetcher;
        this.passwordEncoder = passwordEncoder;
        this.authoritiesFetcher = authoritiesFetcher;
    }


    /**
     * Authenticates a user name and password request.
     *
     * @param requestContext        the current HTTP request, when one is available
     * @param authenticationRequest the submitted username and password
     * @return a publisher that emits a successful authentication or fails with an authentication exception
     */
    @Override
    public @NonNull AuthenticationResponse authenticate(
            @Nullable HttpRequest<B> requestContext,
            @NonNull AuthenticationRequest<String, String> authenticationRequest) {

            UserState user = fetchUserState(authenticationRequest);
            AuthenticationFailed authenticationFailed = validate(user, authenticationRequest);
            if (authenticationFailed != null) {
                return authenticationFailed;
            } else {
                return createSuccessfulAuthenticationResponse(user);
            }
    }

    /**
     * Validates account state and password credentials.
     *
     * @param user the loaded user state, or {@code null} when the user was not found
     * @param authenticationRequest the submitted credentials
     * @return the failure reason, or {@code null} when validation succeeds
     */
    private AuthenticationFailed validate(UserState user, AuthenticationRequest<?, ?> authenticationRequest) {
        AuthenticationFailed authenticationFailed = null;
        if (user == null) {
            authenticationFailed = new AuthenticationFailed(USER_NOT_FOUND);

        } else if (!user.enabled()) {
            authenticationFailed = new AuthenticationFailed(USER_DISABLED);

        } else if (user.expired()) {
            authenticationFailed = new AuthenticationFailed(ACCOUNT_EXPIRED);

        } else if (user.locked()) {
            authenticationFailed = new AuthenticationFailed(ACCOUNT_LOCKED);

        } else if (user.passwordExpired()) {
            authenticationFailed = new AuthenticationFailed(PASSWORD_EXPIRED);

        } else if (!passwordEncoder.matches(authenticationRequest.getSecret().toString(), user.password())) {
            authenticationFailed = new AuthenticationFailed(CREDENTIALS_DO_NOT_MATCH);
        }

        return authenticationFailed;
    }

    /**
     * Loads user state for the submitted identity.
     *
     * @param authRequest the submitted credentials
     * @return the user state, or {@code null} when the user does not exist
     */
    private UserState fetchUserState(AuthenticationRequest<?, ?> authRequest) {
        final Object username = authRequest.getIdentity();
        return userFetcher.findByUsername(username.toString()).orElse(null);
    }

    /**
     * Builds a Micronaut Security response for a verified user.
     *
     * @param user the authenticated user state
     * @return an authenticated response containing the user's authorities
     */
    private AuthenticationResponse createSuccessfulAuthenticationResponse(UserState user) {
        List<String> authorities = authoritiesFetcher.findAuthoritiesByUsername(user.username());
        return AuthenticationResponse.success(user.username(), authorities);
    }
}
