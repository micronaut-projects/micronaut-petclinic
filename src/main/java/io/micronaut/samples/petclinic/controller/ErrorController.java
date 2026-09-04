package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.views.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller for handling errors.
 * Provides custom error pages for different HTTP status codes.
 */
@Controller("/error")
public class ErrorController {

    private final static Logger LOG = LoggerFactory.getLogger(ErrorController.class);

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
        LOG.error("Error :", throwable);
        return Map.of(
                "path", request.getPath(),
                "message", HttpStatus.INTERNAL_SERVER_ERROR.getReason(),
                "exception", HttpStatus.INTERNAL_SERVER_ERROR.getReason()
        );
    }
}
