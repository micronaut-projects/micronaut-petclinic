package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.cookie.Cookie;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * Controller for handling locale/language switching.
 * Stores the user's language preference in a cookie.
 */
@Controller("/locale")
public class LocaleController {

    private static final String LOCALE_COOKIE_NAME = "locale";
    private static final Duration COOKIE_MAX_AGE = Duration.ofDays(365);

    /**
     * Switch the application locale.
     *
     * @param lang the language code (en, es, de)
     * @param request the HTTP request (used to get Referer header)
     * @return redirect to referring page or home with locale cookie set
     */
    @Get
    public HttpResponse<?> changeLocale(@QueryValue(defaultValue = "en") String lang,
                                        HttpRequest<?> request) {
        // Validate language code
        String validLang = switch (lang.toLowerCase()) {
            case "es" -> "es";
            case "de" -> "de";
            default -> "en";
        };

        Cookie localeCookie = Cookie.of(LOCALE_COOKIE_NAME, validLang)
                .maxAge(COOKIE_MAX_AGE)
                .path("/")
                .httpOnly(true);

        Optional<String> referer = Optional.ofNullable(request.getHeaders().get("Referer"));
        String redirectUrl = resolveRedirectUrl(referer.orElse(null));

        return HttpResponse.redirect(URI.create(redirectUrl))
                .cookie(localeCookie);
    }

    /**
     * Check if a URL is internal (safe for redirect).
     * Only allows relative paths or URLs without a host component.
     *
     * @param url the URL to check
     * @return true if the URL is safe for internal redirect
     */
    private String resolveRedirectUrl(String referer) {
        if (referer == null || referer.isBlank()) {
            return "/";
        }

        if (referer.startsWith("/") && !referer.startsWith("//")) {
            return referer.startsWith("/locale") ? "/" : referer;
        }

        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || path.isBlank() || !path.startsWith("/")) {
                return "/";
            }
            if (path.startsWith("/locale")) {
                return "/";
            }

            StringBuilder redirect = new StringBuilder(path);
            if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                redirect.append('?').append(uri.getRawQuery());
            }
            if (uri.getRawFragment() != null && !uri.getRawFragment().isBlank()) {
                redirect.append('#').append(uri.getRawFragment());
            }
            return redirect.toString();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
