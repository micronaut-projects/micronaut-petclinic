package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.samples.petclinic.dto.OwnerForm;
import io.micronaut.samples.petclinic.mapper.FormMapper;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static io.micronaut.samples.petclinic.model.Role.Authority.ROLE_ADMIN_;
import static io.micronaut.samples.petclinic.model.Role.Authority.ROLE_STAFF_;

/**
 * Controller for owner-related operations.
 * Handles CRUD operations and searching for pet owners.
 */
@Controller("/owners")
public class OwnerController {

    private final ClinicService clinicService;
    private final FormMapper formMapper;

    /**
     * Creates the controller with its service and mapper dependencies.
     *
     * @param clinicService the facade used for owner operations
     * @param formMapper the mapper used to convert owner forms
     */
    public OwnerController(ClinicService clinicService, FormMapper formMapper) {
        this.clinicService = clinicService;
        this.formMapper = formMapper;
    }

    /**
     * Display the owner search form.
     *
     * @param notFound whether no owners were found in previous search
     * @return the search form view
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/find")
    @View("owners/findOwners")
    public Map<String, Object> initFindForm(@QueryValue(defaultValue = "false") Boolean notFound) {
        return Map.of(
                "owner", new Owner(),
                "notFound", notFound
        );
    }

    /**
     * Process the owner search form.
     *
     * @param lastName the last name to search for
     * @return redirect to owner details or list of matching owners
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
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
            URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", owner.id()));
            return HttpResponse.redirect(uri);
        } else {
            // Multiple owners found - show list
            return HttpResponse.ok(Map.of(
                    "owners", results,
                    "lastName", lastName
            ));
        }
    }

    /**
     * Display the list of owners matching search criteria.
     *
     * @param lastName the last name filter
     * @return the owner list view
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/list")
    @View("owners/ownersList")
    public Map<String, Object> showOwnerList(@QueryValue(defaultValue = "") String lastName) {
        Collection<Owner> owners;
        if (lastName.isEmpty()) {
            owners = clinicService.findAllOwners();
        } else {
            owners = clinicService.findOwnerByLastName(lastName);
        }

        return Map.of(
                "owners", owners,
                "lastName", lastName
        );
    }

    /**
     * Display owner details.
     *
     * @param ownerId the owner ID
     * @return the owner details view
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/{ownerId}")
    @View("owners/ownerDetails")
    public Map<String, Object> showOwner(@PathVariable Integer ownerId) {
        Owner owner = clinicService.findOwnerById(ownerId).orElseThrow(NotFoundException::new);
        return Map.of("owner", owner);
    }

    /**
     * Display the form to create a new owner.
     *
     * @return the create owner form view
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/new")
    @View("owners/createOrUpdateOwnerForm")
    public Map<String, Object> initCreationForm() {
        return Map.of(
                "owner", new OwnerForm(),
                "isNew", true,
                "validationErrors", Map.of()
        );
    }

    /**
     * Process the form to create a new owner.
     *
     * @param form the owner form data
     * @return redirect to owner details on success, or form with errors
     */
    @Secured(ROLE_STAFF_)
    @Post(value = "/new", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processCreationForm(@Valid @Body OwnerForm form) {
        Owner owner = formMapper.toOwner(form);
        Owner savedOwner = clinicService.saveOwner(owner);
        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", savedOwner.id()));
        return HttpResponse.redirect(uri);
    }

    /**
     * Renders the owner creation form when validation fails.
     *
     * @param request the request containing the submitted form
     * @param e the validation exception
     * @return a form model containing submitted values and field errors
     */
    @io.micronaut.http.annotation.Error(exception = ConstraintViolationException.class)
    @View("owners/createOrUpdateOwnerForm")
    public Map<String, Object> onCreateOwnerValidationError(HttpRequest<?> request,
                                                            ConstraintViolationException e) {
        // For form posts, render the form again instead of sending users to the generic 500 page.
        OwnerForm owner = request.getBody(OwnerForm.class).orElseGet(OwnerForm::new);

        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            String field = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
            // For form binding, Micronaut prefixes with method + param, e.g. "processCreationForm.form.telephone".
            // We only need the leaf field name to show inline messages.
            int lastDot = field.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < field.length() - 1) {
                field = field.substring(lastDot + 1);
            }
            errors.put(field, v.getMessage());
        }
        return Map.of(
                "owner", owner,
                "isNew", true,
                "validationErrors", errors
        );
    }

    /**
     * Display the form to edit an existing owner.
     *
     * @param ownerId the owner ID
     * @return the edit owner form view
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/{ownerId}/edit")
    @View("owners/createOrUpdateOwnerForm")
    public Map<String, Object> initUpdateOwnerForm(@PathVariable Integer ownerId) {
        Owner owner = clinicService.findOwnerById(ownerId).orElseThrow(NotFoundException::new);
            return Map.of(
                    "owner", formMapper.toOwnerForm(owner),
                    "ownerId", ownerId,
                    "isNew", false,
                    "validationErrors", Map.of()
            );
        }


    /**
     * Process the form to update an existing owner.
     *
     * @param ownerId the owner ID
     * @param form    the updated owner form data
     * @return redirect to owner details on success
     */
    @Secured({ROLE_STAFF_, ROLE_ADMIN_})
    @Post(value = "/{ownerId}/edit", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processUpdateOwnerForm(@PathVariable Integer ownerId, @Valid @Body OwnerForm form) {
        Optional<Owner> existingOwner = clinicService.findOwnerById(ownerId);
        if (existingOwner.isEmpty()) {
            return HttpResponse.notFound();
        }

        Owner owner = formMapper.updateOwner(existingOwner.get(), form);
        clinicService.saveOwner(owner);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        return HttpResponse.redirect(uri);
    }
}
