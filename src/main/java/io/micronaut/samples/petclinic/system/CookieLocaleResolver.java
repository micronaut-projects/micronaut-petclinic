package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.http.server.util.locale.HttpLocaleResolver;
import jakarta.inject.Singleton;

import java.util.Locale;
import java.util.Optional;

/**
 * Custom locale resolver that reads the user's locale preference from a cookie.
 * Falls back to the Accept-Language header if no cookie is set.
 */
@Singleton
@Replaces(HttpLocaleResolver.class)
public class CookieLocaleResolver implements HttpLocaleResolver {

    private static final String LOCALE_COOKIE_NAME = "locale";
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * Creates the cookie-backed locale resolver.
     */
    public CookieLocaleResolver() {
    }

    /**
     * Resolves the request locale from the locale cookie, falling back to the request locale.
     *
     * @param request the current HTTP request
     * @return the resolved locale, if one can be determined
     */
    @Override
    @NonNull
    public Optional<Locale> resolve(@NonNull HttpRequest<?> request) {
        // First, try to get locale from cookie
        Optional<Cookie> localeCookie = request.getCookies().findCookie(LOCALE_COOKIE_NAME);

        if (localeCookie.isPresent()) {
            String lang = localeCookie.get().getValue();
            if (StringUtils.isNotEmpty(lang)) {
                return Optional.of(Locale.forLanguageTag(lang));
            }
        }

        // Fall back to Accept-Language header
        return request.getLocale();
    }

    /**
     * Resolves the request locale, returning English when no locale is available.
     *
     * @param request the current HTTP request
     * @return the resolved locale or the default locale
     */
    @Override
    @NonNull
    public Locale resolveOrDefault(@NonNull HttpRequest<?> request) {
        return resolve(request).orElse(DEFAULT_LOCALE);
    }
}
