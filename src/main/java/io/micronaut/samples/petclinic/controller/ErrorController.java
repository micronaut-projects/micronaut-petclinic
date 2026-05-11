package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.views.View;
import java.util.Map;
import java.util.Objects;

/**
 * Controller for handling errors.
 * Provides custom error pages for different HTTP status codes.
 */
@Controller("/error")
public class ErrorController {

    /**
     * Creates the error controller.
     */
    public ErrorController() {
    }

    /**
     * Handle 404 Not Found errors.
     * @param request the original request
     * @return the error view
     */
    @Error(status = io.micronaut.http.HttpStatus.NOT_FOUND, global = true)
    @View("error/404")
    public Map<String, Object> notFound(HttpRequest<?> request) {
        return Map.of(
                "path", request.getPath(),
                "message", "Page not found"
        );
    }

    /**
     * Handle 500 Internal Server Error.
     * @param request the original request
     * @param throwable the exception that occurred
     * @return the error view
     */
    @Error(global = true)
    @View("error/error")
    public Map<String, Object> handleError(HttpRequest<?> request, Throwable throwable) {
        return Map.of(
                "path", request.getPath(),
                "message", Objects.toString(throwable.getMessage(), ""),
                "exception", throwable.getClass().getSimpleName()
        );
    }
}
