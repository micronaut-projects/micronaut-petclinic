package io.micronaut.samples.petclinic.controller;

import io.micronaut.core.convert.exceptions.ConversionErrorException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.exceptions.ContentLengthExceededException;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.json.JsonSyntaxException;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.InvalidFileFormatException;
import io.micronaut.samples.petclinic.exception.FileTooLargeException;
import io.micronaut.samples.petclinic.exception.RequestConflictException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.exception.PreconditionFailedException;
import io.micronaut.samples.petclinic.exception.PreconditionRequiredException;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.micronaut.http.HttpStatus.NOT_FOUND;
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
     * Reports invalid request data not handled by a more specific exception handler.
     *
     * @param error the request failure
     * @return a client-safe message with HTTP 400
     */
    @Error(exception = IllegalRequestArgumentException.class, global = true)
    @Status(HttpStatus.BAD_REQUEST)
    public Map<String, String> validationError(IllegalRequestArgumentException error) {
        return Map.of("message", message(error, "Invalid request."));
    }

    /**
     * Reports an invalid file format.
     *
     * @param error the request failure
     * @return a stable error code and client-safe message with HTTP 400
     */
    @Error(exception = InvalidFileFormatException.class, global = true)
    @Status(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidFileFormat(InvalidFileFormatException error) {
        return Map.of("code", "invalid_file_format", "message", error.getMessage());
    }

    /**
     * Reports a file size limit violation.
     *
     * @param error the request failure
     * @return a stable error code and client-safe message with HTTP 413
     */
    @Error(exception = FileTooLargeException.class, global = true)
    @Status(HttpStatus.REQUEST_ENTITY_TOO_LARGE)
    public Map<String, String> fileTooLarge(FileTooLargeException error) {
        return Map.of("code", "file_too_large", "message", error.getMessage());
    }

    /**
     * Reports a concurrent request conflict.
     *
     * @param error the request failure
     * @return a stable error code and client-safe message with HTTP 409
     */
    @Error(exception = RequestConflictException.class, global = true)
    @Status(HttpStatus.CONFLICT)
    public Map<String, String> requestConflict(RequestConflictException error) {
        return Map.of("code", "request_conflict", "message", error.getMessage());
    }

    /**
     * Reports a request that omitted a required precondition.
     *
     * @param error the missing-precondition failure
     * @return HTTP 428 with a JSON message
     */
    @Error(exception = PreconditionRequiredException.class, global = true)
    @Status(PRECONDITION_REQUIRED)
    public Map<String, String> preconditionRequired(PreconditionRequiredException error) {
        return Map.of("message", message(error, "Provide the current ETag."));
    }

    /**
     * Reports a request whose supplied precondition failed.
     *
     * @param error the failed-precondition failure
     * @return HTTP 412 with a JSON message
     */
    @Error(exception = PreconditionFailedException.class, global = true)
    @Status(PRECONDITION_FAILED)
    public Map<String, String> preconditionFailed(PreconditionFailedException error) {
        return Map.of("message", message(error, "The supplied precondition failed."));
    }

    /**
     * Reports JSON syntax errors without exposing parser internals.
     *
     * @param error the request failure
     * @return HTTP 400 with a JSON message
     */
    @Error(exception = JsonSyntaxException.class, global = true)
    @Status(HttpStatus.BAD_REQUEST)
    public Map<String, String> malformedJson(JsonSyntaxException error) {
        return Map.of("message", "Malformed JSON request.");
    }

    /**
     * Reports request values that cannot be converted to the required type.
     *
     * @param error the request failure
     * @return HTTP 400 with a JSON message
     */
    @Error(exception = ConversionErrorException.class, global = true)
    @Status(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidValue(ConversionErrorException error) {
        return Map.of("message", "Invalid request value.");
    }

    /**
     * Returns HTTP 413 when a multipart part exceeds Micronaut's configured limit.
     *
     * @param error the multipart size failure
     * @return a payload-too-large response
     */
    @Error(exception = ContentLengthExceededException.class, global = true)
    @Status(HttpStatus.REQUEST_ENTITY_TOO_LARGE)
    public Map<String, String> contentTooLarge(ContentLengthExceededException error) {
        return Map.of("code", "file_too_large", "message", "The uploaded file exceeds the maximum size.");
    }

    /**
     * Reports a requested domain resource that does not exist.
     *
     * @param request the request whose path is included in the response
     * @param e the missing-entity failure
     * @return HTTP 404 with a JSON message
     */
    @Error(exception = NotFoundEntityException.class, global = true)
    @Status(NOT_FOUND)
    public Map<String, String> missingResource(HttpRequest<?> request, NotFoundEntityException e) {
        return Map.of(
                "path", request.getPath(),
                "message", e.getMessage()
        );
    }

    /**
     * Handle 404 Not Found errors.
     * @param request the original request
     * @return the error view
     */
    @Error(exception = NotFoundException.class, global = true)
    @View("error/404")
    @Status(NOT_FOUND)
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
