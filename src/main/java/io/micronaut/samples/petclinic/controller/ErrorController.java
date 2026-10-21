package io.micronaut.samples.petclinic.controller;

import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.json.JsonSyntaxException;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.PreconditionFailedException;
import io.micronaut.samples.petclinic.exception.PreconditionRequiredException;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.micronaut.http.HttpStatus.PRECONDITION_FAILED;
import static io.micronaut.http.HttpStatus.PRECONDITION_REQUIRED;

/**
 * Controller for handling errors.
 * Provides custom error pages for different HTTP status codes.
 */
@Controller("/error")
@Secured(SecurityRule.IS_ANONYMOUS)
public class ErrorController {

    private final static Logger LOG = LoggerFactory.getLogger(ErrorController.class);

    /**
     * Handle authorization failures raised by the security filter.
     *
     * @param request the original request
     * @return the error view with the correct status
     */
    @Error(exception = AuthorizationException.class, global = true)
    @View("error/401")
    public Map<String, Object> unauthorised(HttpRequest<?> request) {
        return Map.of(
                "path", request.getPath(),
                "message", "unauthorized"
        );
    }

    @Error(status = HttpStatus.FORBIDDEN, global = true)
    @View("error/401")
    public Map<String, Object> forbidden(HttpRequest<?> request) {
        return Map.of(
                "path", request.getPath(),
                "message", "Forbidden"
        );
    }

    /**
     * Reports invalid request data.
     *
     * @param error the request failure
     * @return HTTP 400 with a JSON message
     */
    @Error(exception = ConstraintViolationException.class, global = true)
    public HttpResponse<Map<String, String>> validationError(ConstraintViolationException error) {
        return HttpResponse.badRequest(Map.of("message", message(error, "Invalid request.")));
    }

    /**
     * Reports bean-validation failures from any controller.
     *
     * @param error the request failure
     * @return HTTP response with a JSON message and status.
     */
    @Error(exception = IllegalRequestArgumentException.class, global = true)
    public HttpResponse<Map<String, String>> validationError(IllegalRequestArgumentException error) {
        return HttpResponse.badRequest(Map.of("message", message(error, "Invalid request.")));
    }

    /**
     * Reports a request that omitted a required precondition.
     *
     * @param error the missing-precondition failure
     * @return HTTP 428 with a JSON message
     */
    @Error(exception = PreconditionRequiredException.class, global = true)
    public HttpResponse<Map<String, String>> preconditionRequired(PreconditionRequiredException error) {
        return HttpResponse.status(PRECONDITION_REQUIRED)
                .body(Map.of("message", message(error, "Provide the current ETag.")));
    }

    /**
     * Reports a request whose supplied precondition failed.
     *
     * @param error the failed-precondition failure
     * @return HTTP 412 with a JSON message
     */
    @Error(exception = PreconditionFailedException.class, global = true)
    public HttpResponse<Map<String, String>> preconditionFailed(PreconditionFailedException error) {
        return HttpResponse.status(PRECONDITION_FAILED)
                .body(Map.of("message", message(error, "The supplied precondition failed.")));
    }

    /**
     * Reports JSON syntax errors without exposing parser internals.
     *
     * @param error the request failure
     * @return HTTP 400 with a JSON message
     */
    @Error(exception = JsonSyntaxException.class, global = true)
    public HttpResponse<Map<String, String>> malformedJson(JsonSyntaxException error) {
        return HttpResponse.badRequest(Map.of("message", "Malformed JSON request."));
    }

    /**
     * Reports request values that cannot be converted to the required type.
     *
     * @param error the request failure
     * @return HTTP 400 with a JSON message
     */
    @Error(exception = ConversionErrorException.class, global = true)
    public HttpResponse<Map<String, String>> invalidValue(ConversionErrorException error) {
        return HttpResponse.badRequest(Map.of("message", "Invalid request value."));
    }

    /**
     * Reports a requested domain resource that does not exist.
     *
     * @param error the missing-resource failure
     * @return HTTP 404 with a JSON message
     */
    @Error(exception = NotFoundException.class, global = true)
    @View("error/404")
    public Map<String, String> missingResource(HttpRequest<?> request, NotFoundException error) {
        return Map.of(
                "path", request.getPath(),
                "message", "Page not found"
        );
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

    private static String message(Throwable error, String fallback) {
        return error.getMessage() == null || error.getMessage().isBlank() ? fallback : error.getMessage();
    }
}
