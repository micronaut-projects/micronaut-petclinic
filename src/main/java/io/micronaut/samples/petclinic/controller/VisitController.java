package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.samples.petclinic.dto.VisitForm;
import io.micronaut.samples.petclinic.mapper.FormMapper;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for visit-related operations.
 * Handles creating visits for pets.
 */
@Controller("/owners/{ownerId}/pets/{petId}/visits")
public class VisitController {

    private final ClinicService clinicService;
    private final FormMapper formMapper;

    /**
     * Creates the controller with its service and mapper dependencies.
     *
     * @param clinicService the facade used for visit operations
     * @param formMapper the mapper used to convert visit forms
     */
    public VisitController(ClinicService clinicService, FormMapper formMapper) {
        this.clinicService = clinicService;
        this.formMapper = formMapper;
    }

    /**
     * Renders the visit form when validation fails.
     *
     * @param request the request containing the submitted form
     * @param e the validation exception
     * @return a form model containing submitted values and field errors
     */
    @io.micronaut.http.annotation.Error(exception = ConstraintViolationException.class)
    @View("pets/createOrUpdateVisitForm")
    public Map<String, Object> onConstraintViolation(HttpRequest<?> request,
                                                     ConstraintViolationException e) {
        Integer ownerId = request.getParameters().get("ownerId", Integer.class).orElse(null);
        Integer petId = request.getParameters().get("petId", Integer.class).orElse(null);

        Optional<Pet> pet = petId != null ? clinicService.findPetById(petId) : Optional.empty();

        Owner owner = null;
        if (pet.isPresent() && pet.get().getOwner() != null) {
            owner = pet.get().getOwner();
        }

        Map<String, String> validationErrors = new LinkedHashMap<>();
        for (var violation : e.getConstraintViolations()) {
            String field = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "";
            int lastDot = field.lastIndexOf('.');
            if (lastDot >= 0) {
                field = field.substring(lastDot + 1);
            }
            if (!field.isBlank()) {
                validationErrors.put(field, violation.getMessage());
            }
        }

        VisitForm form = request.getBody(VisitForm.class).orElseGet(VisitForm::new);
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("pet", pet.orElse(null));
        model.put("owner", owner);
        model.put("validationErrors", validationErrors);
        model.put("visit", form);

        return model;
    }

    /**
     * Display the form to create a new visit.
     *
     * @param ownerId the owner ID
     * @param petId   the pet ID
     * @return the create visit form view
     */
    @Get("/new")
    @View("pets/createOrUpdateVisitForm")
    public Map<String, Object> initNewVisitForm(@PathVariable Integer ownerId, @PathVariable Integer petId) {
        Pet pet = clinicService.findPetById(petId).orElseThrow(NotFoundException::new);
        return Map.of("visit", new VisitForm(),
                "pet", pet,
                "owner", pet.getOwner(),
                "validationErrors", Map.of()
        );
    }

    /**
     * Process the form to create a new visit.
     *
     * @param ownerId the owner ID
     * @param petId   the pet ID
     * @param form    the visit form data
     * @return redirect to owner details
     */
    @Post(value = "/new", consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<?> processNewVisitForm(@PathVariable Integer ownerId,
                                                @PathVariable Integer petId,
                                                @Valid @Body VisitForm form) {
        Optional<Pet> pet = clinicService.findPetById(petId);

        if (pet.isEmpty()) {
            return HttpResponse.notFound();
        }

        Visit visit = formMapper.toVisit(form).withPet(pet.get());
        clinicService.saveVisit(visit);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        return HttpResponse.redirect(uri);
    }
}
