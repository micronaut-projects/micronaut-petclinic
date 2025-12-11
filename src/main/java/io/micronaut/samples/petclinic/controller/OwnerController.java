package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.samples.petclinic.dto.OwnerForm;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.views.View;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.*;

/**
 * Controller for owner-related operations.
 * Handles CRUD operations and searching for pet owners.
 */
@Controller("/owners")
public class OwnerController {

    private final ClinicService clinicService;

    public OwnerController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    /**
     * Display the owner search form.
     *
     * @param notFound whether no owners were found in previous search
     * @return the search form view
     */
    @Get("/find")
    @View("owners/findOwners")
    public Map<String, Object> initFindForm(@QueryValue(defaultValue = "false") Boolean notFound) {
        Map<String, Object> model = new HashMap<>();
        model.put("owner", new Owner());
        model.put("notFound", notFound);
        return model;
    }

    /**
     * Process the owner search form.
     *
     * @param lastName the last name to search for
     * @return redirect to owner details or list of matching owners
     */
    @Get
    public HttpResponse<?> processFindForm(@QueryValue(defaultValue = "") String lastName) {
        Collection<Owner> results;

        if (lastName.isEmpty()) {
            results = clinicService.findAllOwners();
        } else {
            results = clinicService.findOwnerByLastName(lastName);
        }

        if (results.isEmpty()) {
            // No owners found - return to search with error
            URI uri = UriBuilder.of("/owners/find").queryParam("notFound", true).build();
            return HttpResponse.redirect(uri);
        } else if (results.size() == 1) {
            // Single owner found - redirect to owner details
            Owner owner = results.iterator().next();
            URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", owner.getId()));
            return HttpResponse.redirect(uri);
        } else {
            // Multiple owners found - show list
            Map<String, Object> model = new HashMap<>();
            model.put("owners", results);
            model.put("lastName", lastName);
            return HttpResponse.ok(model);
        }
    }

    /**
     * Display the list of owners matching search criteria.
     *
     * @param lastName the last name filter
     * @return the owner list view
     */
    @Get("/list")
    @View("owners/ownersList")
    public Map<String, Object> showOwnerList(@QueryValue(defaultValue = "") String lastName) {
        Collection<Owner> owners;
        if (lastName.isEmpty()) {
            owners = clinicService.findAllOwners();
        } else {
            owners = clinicService.findOwnerByLastName(lastName);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("owners", owners);
        model.put("lastName", lastName);
        return model;
    }

    /**
     * Display owner details.
     *
     * @param ownerId the owner ID
     * @return the owner details view
     */
    @Get("/{ownerId}")
    @View("owners/ownerDetails")
    public Map<String, Object> showOwner(@PathVariable Integer ownerId) {
        Map<String, Object> model = new HashMap<>();
        Optional<Owner> owner = clinicService.findOwnerById(ownerId);
        if (owner.isPresent()) {
            model.put("owner", owner.get());
        } else {
            model.put("error", "Owner not found");
        }
        return model;
    }

    /**
     * Display the form to create a new owner.
     *
     * @return the create owner form view
     */
    @Get("/new")
    @View("owners/createOrUpdateOwnerForm")
    public Map<String, Object> initCreationForm() {
        Map<String, Object> model = new HashMap<>();
        model.put("owner", new OwnerForm());
        model.put("isNew", true);
        return model;
    }

    /**
     * Process the form to create a new owner.
     *
     * @param form the owner form data
     * @return redirect to owner details on success, or form with errors
     */
    @Post(value = "/new", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processCreationForm(@Valid @Body OwnerForm form) {
        Owner owner = form.toOwner();
        Owner savedOwner = clinicService.saveOwner(owner);
        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", savedOwner.getId()));
        return HttpResponse.redirect(uri);
    }

    /**
     * Display the form to edit an existing owner.
     *
     * @param ownerId the owner ID
     * @return the edit owner form view
     */
    @Get("/{ownerId}/edit")
    @View("owners/createOrUpdateOwnerForm")
    public Map<String, Object> initUpdateOwnerForm(@PathVariable Integer ownerId) {
        Map<String, Object> model = new HashMap<>();
        Optional<Owner> owner = clinicService.findOwnerById(ownerId);
        if (owner.isPresent()) {
            model.put("owner", OwnerForm.fromOwner(owner.get()));
            model.put("ownerId", ownerId);
            model.put("isNew", false);
        } else {
            model.put("error", "Owner not found");
            model.put("isNew", false);
        }
        return model;
    }

    /**
     * Process the form to update an existing owner.
     *
     * @param ownerId the owner ID
     * @param form    the updated owner form data
     * @return redirect to owner details on success
     */
    @Post(value = "/{ownerId}/edit", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processUpdateOwnerForm(@PathVariable Integer ownerId, @Valid @Body OwnerForm form) {
        Optional<Owner> existingOwner = clinicService.findOwnerById(ownerId);
        if (existingOwner.isEmpty()) {
            return HttpResponse.notFound();
        }

        Owner owner = form.updateOwner(existingOwner.get());
        clinicService.saveOwner(owner);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        return HttpResponse.redirect(uri);
    }
}
