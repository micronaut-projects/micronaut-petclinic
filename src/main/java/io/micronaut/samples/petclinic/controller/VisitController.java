package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.samples.petclinic.dto.VisitForm;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.views.View;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.*;

/**
 * Controller for visit-related operations.
 * Handles creating visits for pets.
 */
@Controller("/owners/{ownerId}/pets/{petId}/visits")
public class VisitController {

    private final ClinicService clinicService;

    public VisitController(ClinicService clinicService) {
        this.clinicService = clinicService;
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
        Map<String, Object> model = new HashMap<>();
        Optional<Pet> pet = clinicService.findPetById(petId);

        if (pet.isPresent()) {
            model.put("visit", new VisitForm());
            model.put("pet", pet.get());
            model.put("owner", pet.get().getOwner());
        } else {
            model.put("error", "Pet not found");
        }
        return model;
    }

    /**
     * Process the form to create a new visit.
     *
     * @param ownerId the owner ID
     * @param petId   the pet ID
     * @param form    the visit form data
     * @return redirect to owner details
     */
    @Post(value = "/new", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processNewVisitForm(@PathVariable Integer ownerId,
                                                @PathVariable Integer petId,
                                                @Valid @Body VisitForm form) {
        Optional<Pet> pet = clinicService.findPetById(petId);

        if (pet.isEmpty()) {
            return HttpResponse.notFound();
        }

        Visit visit = form.toVisit();
        pet.get().addVisit(visit);
        clinicService.saveVisit(visit);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        return HttpResponse.redirect(uri);
    }
}
