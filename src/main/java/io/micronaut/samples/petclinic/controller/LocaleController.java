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
                .httpOnly(false);

        // Get referer from HTTP header, default to home page
        Optional<String> referer = request.getHeaders().get("Referer").describeConstable();
        String redirectUrl = referer.orElse("/");
        
        // If referer is the locale endpoint itself, redirect to home
        if (redirectUrl.contains("/locale")) {
            redirectUrl = "/";
        }

        return HttpResponse.redirect(URI.create(redirectUrl))
                .cookie(localeCookie);
    }
}