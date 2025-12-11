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

    @Override
    @NonNull
    public Locale resolveOrDefault(@NonNull HttpRequest<?> request) {
        return resolve(request).orElse(DEFAULT_LOCALE);
    }
}