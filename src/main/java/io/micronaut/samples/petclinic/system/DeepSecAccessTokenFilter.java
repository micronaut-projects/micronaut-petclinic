package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.context.SecurityContextHolder;
import io.micronaut.security.filters.SecurityFilter;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes the original Entra access token available to the Oracle JDBC
 * end-user security-context provider.
 *
 * <p>Cookie authentication normally authenticates requests with a Micronaut
 * application JWT. The Oracle provider must receive the original Entra
 * access token instead, because that token contains the user's IAM identity
 * and application roles. The token is stored as an authentication claim by
 * the OAuth2 mapper and is copied to the request security-context attribute
 * before the controller runs.</p>
 */
@Requires(env = "oracle-deepsec")
@Filter(patterns = "/**")
public final class DeepSecAccessTokenFilter implements HttpServerFilter {

    private static final String ACCESS_TOKEN_ATTRIBUTE = "accessToken";
    private static final Logger LOG = LoggerFactory.getLogger(DeepSecAccessTokenFilter.class);

    /**
     * Run after Micronaut's security filter has resolved the browser cookie.
     *
     * @return an order after the security phase
     */
    @Override
    public int getOrder() {
        return ServerFilterPhase.SECURITY.after();
    }

    /**
     * Replace the application-cookie token only for the downstream JDBC
     * provider; the authenticated principal and its roles remain unchanged.
     *
     * @param request the current HTTP request
     * @param chain the remaining filter chain
     * @return the downstream response publisher
     */
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request,
                                                       ServerFilterChain chain) {
        Authentication authentication = SecurityContextHolder.getSecurityContext().getAuthentication();
        if (authentication != null) {
            Object accessToken = authentication.getAttributes().get(ACCESS_TOKEN_ATTRIBUTE);
            if (accessToken instanceof String token && !token.isBlank()) {
                request.setAttribute(SecurityFilter.TOKEN, token);
                LOG.debug("DeepSec token bridge: {} {} authenticated=true endUserAccessTokenPresent=true",
                        request.getMethod(), request.getPath());
            } else {
                LOG.debug("DeepSec token bridge: {} {} authenticated=true endUserAccessTokenPresent=false",
                        request.getMethod(), request.getPath());
            }
        } else {
            LOG.debug("DeepSec token bridge: {} {} authenticated=false endUserAccessTokenPresent=false",
                    request.getMethod(), request.getPath());
        }
        return chain.proceed(request);
    }
}
