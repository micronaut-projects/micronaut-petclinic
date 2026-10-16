package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.json.JsonSyntaxException;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.MissingEtagException;
import io.micronaut.samples.petclinic.exception.StaleEtagException;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
     * Reports an invalid domain request.
     *
     * @param e the request failure
     * @return HTTP 400 with a JSON message
     */
    @Error(exception = IllegalRequestArgumentException.class, global = true)
    public static HttpResponse<?> IllegalRequestArgumentExceptionHandler(IllegalRequestArgumentException e) {
        return HttpResponse.badRequest().body(Map.of("message", message(e, "Invalid request.")));
    }

    /**
     * Reports bean-validation failures from any controller.
     *
     * @param error the request failure
     * @return HTTP 400 with a JSON message
     */
    @Error(exception = ConstraintViolationException.class, global = true)
    public HttpResponse<Map<String, String>> validationError(ConstraintViolationException error) {
        return HttpResponse.badRequest(Map.of("message", message(error, "Invalid request.")));
    }

    /**
     * Reports a mutation without its required ETag.
     *
     * @param e the request failure
     * @return HTTP 428 with a JSON message
     */
    @Error(exception = MissingEtagException.class, global = true)
    public static HttpResponse<?> MissingEtagExceptionHandler(MissingEtagException e) {
        return HttpResponse.status(PRECONDITION_REQUIRED).body(Map.of("message", message(e, "Provide the current ETag.")));
    }

    /**
     * Reports a rejected optimistic-locking precondition.
     *
     * @param error the request failure
     * @return HTTP 412 with a JSON message
     */
    @Error(exception = StaleEtagException.class, global = true)
    public HttpResponse<Map<String, String>> staleVet(StaleEtagException error) {
        return HttpResponse.status(HttpStatus.PRECONDITION_FAILED).body(Map.of("message", error.getMessage()));
    }

    /**
     * Preserves the status of an HTTP exception raised by a controller.
     *
     * @param error the request failure
     * @return the exception status and a JSON message
     */
    @Error(exception = HttpStatusException.class, global = true)
    public HttpResponse<Map<String, String>> statusError(HttpStatusException error) {
        return HttpResponse.status(error.getStatus())
                .body(Map.of("message", message(error, error.getStatus().getReason())));
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
