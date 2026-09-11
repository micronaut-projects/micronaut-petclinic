package io.micronaut.samples.petclinic.system;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.micronaut.context.annotation.Requires;
import io.micronaut.security.config.AuthenticationModeConfiguration;
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration;
import io.micronaut.security.oauth2.endpoint.token.response.DefaultOpenIdAuthenticationMapper;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Carries the PetClinic app roles from the Entra access token into the
 * Micronaut authentication stored in the browser cookie.
 *
 * <p>Micronaut's default OpenID mapper creates a new application
 * authentication and does not infer roles from the provider access token.
 * DeepSec still needs the original token for Oracle, but the application also
 * needs the mapped {@code STAFF} role to protect the support endpoint and its
 * UI control.</p>
 */
@Requires(env = "oracle-deepsec")
@Singleton
@Named("entraid")
public final class PetClinicOpenIdAuthenticationMapper extends DefaultOpenIdAuthenticationMapper {

    /**
     * Creates the mapper used for the Entra OAuth client.
     *
     * @param additionalClaimsConfiguration configuration for copied provider claims
     * @param authenticationModeConfiguration Micronaut authentication mode
     */
    public PetClinicOpenIdAuthenticationMapper(
            OpenIdAdditionalClaimsConfiguration additionalClaimsConfiguration,
            AuthenticationModeConfiguration authenticationModeConfiguration) {
        super(additionalClaimsConfiguration, authenticationModeConfiguration);
    }

    @Override
    protected List<String> getRoles(String providerName,
                                    OpenIdTokenResponse tokenResponse,
                                    OpenIdClaims openIdClaims) {
        Set<String> roles = new LinkedHashSet<>(super.getRoles(providerName, tokenResponse, openIdClaims));
        addRoles(roles, openIdClaims.get("roles"));

        try {
            JWT accessToken = JWTParser.parse(tokenResponse.getAccessToken());
            addRoles(roles, accessToken.getJWTClaimsSet().getClaim("roles"));
        } catch (ParseException ignored) {
            // Oracle will reject an invalid end-user token when the database
            // context is created; do not fail the browser callback only because
            // this optional role projection could not be parsed.
        }

        return List.copyOf(roles);
    }

    private static void addRoles(Set<String> roles, Object value) {
        if (value instanceof Collection<?> values) {
            values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .forEach(roles::add);
        } else if (value instanceof String role) {
            String trimmed = role.trim();
            if (!trimmed.isEmpty()) {
                roles.add(trimmed);
            }
        }
    }
}
