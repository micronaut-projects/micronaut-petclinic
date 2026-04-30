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

        // Get referer from HTTP header, default to home page.
        // If a browser/referrer policy strips Referer, we can still support
        // "stay on current page" by letting the client send a relative backUrl.
        String redirectUrl = request.getParameters().get("backUrl", String.class).orElse(null);
        if (redirectUrl == null || redirectUrl.isBlank()) {
            redirectUrl = request.getHeaders().get("Referer");
            if (redirectUrl == null || redirectUrl.isBlank()) {
                redirectUrl = "/";
            }
        }

        // Only allow internal redirects (relative paths only).
        if (!isInternalUrl(redirectUrl) || redirectUrl.contains("/locale")) {
            redirectUrl = "/";
        }

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
    private boolean isInternalUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        // Relative paths are safe
        if (url.startsWith("/") && !url.startsWith("//")) {
            return true;
        }
        // Reject anything that looks like an absolute URL
        if (url.contains("://") || url.startsWith("//")) {
            return false;
        }
        return false;
    }
}
