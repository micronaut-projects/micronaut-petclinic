package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.exceptions.OptimisticLockException;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.samples.petclinic.oracle.model.OwnerCareView;
import io.micronaut.samples.petclinic.service.OwnerCareViewService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.stream.Collectors;

@Controller("/owners/{ownerId}/care-record")
@Requires(env = "oracle")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_ANONYMOUS)
class OwnerCareRecordController {

    private final OwnerCareViewService service;

    public OwnerCareRecordController(OwnerCareViewService service) {
        this.service = service;
    }

    @Get
    public OwnerCareView getDocument(@PathVariable Integer ownerId) {
        return service.findById(ownerId);
    }

    @Put(consumes = MediaType.APPLICATION_JSON)
    public OwnerCareView updateDocument(@PathVariable @NonNull Integer ownerId,
                                        @NotNull @Valid @Body OwnerCareView document) {
        if (!ownerId.equals(document.id())) {
            throw new IllegalArgumentException("The document _id must match the owner in the URL.");
        }
        return service.update(document);
    }

    @Error(exception = OptimisticLockException.class)
    public HttpResponse<Map<String, String>> staleDocument(OptimisticLockException error) {
        return HttpResponse.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "This owner record changed after it was loaded. Reload it before saving."));
    }

    @Error(exception = ConstraintViolationException.class)
    public HttpResponse<Map<String, String>> invalidFields(ConstraintViolationException error) {
        String message = error.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return HttpResponse.badRequest(Map.of("message", message));
    }

    @Error(exception = IllegalArgumentException.class)
    public HttpResponse<Map<String, String>> invalidDocument(IllegalArgumentException error) {
        return HttpResponse.badRequest(Map.of("message", error.getMessage()));
    }

    @Error(exception = NotFoundException.class)
    public HttpResponse<Map<String, String>> notFound(NotFoundException error) {
        return HttpResponse.notFound(Map.of("message", error.getMessage()));
    }
}
