package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.exceptions.OptimisticLockException;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Put;
import io.micronaut.samples.petclinic.model.OwnerCareView;
import io.micronaut.samples.petclinic.service.OwnerCareViewService;
import io.micronaut.samples.petclinic.service.OwnerCareViewService.OwnerCareNotFoundException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.Map;

@Controller("/owners/{ownerId}/json-duality")
@Requires(env = "oracle")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_ANONYMOUS)
public class OracleJsonDualityController {

    private final OwnerCareViewService service;

    public OracleJsonDualityController(OwnerCareViewService service) {
        this.service = service;
    }

    @Get
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> getDocument(@PathVariable Integer ownerId) {
        OwnerCareView document = service.findById(ownerId);
        return document == null ? HttpResponse.notFound() : HttpResponse.ok(document);
    }

    @Error(status = HttpStatus.NOT_FOUND)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<Map<String, String>> documentNotFound(HttpRequest<?> request) {
        return HttpResponse.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Owner care document not found."));
    }

    @Put(consumes = MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> updateDocument(@PathVariable Integer ownerId, @Body OwnerCareView document) {
        try {
            return HttpResponse.ok(service.update(ownerId, document));
        } catch (OwnerCareNotFoundException e) {
            return HttpResponse.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (OptimisticLockException e) {
            return HttpResponse.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "This owner record changed after it was loaded. Reload it before saving."));
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest(Map.of("message", e.getMessage()));
        }
    }
}
