package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.samples.petclinic.dto.OwnerForm;
import io.micronaut.samples.petclinic.dto.OwnerListItem;
import io.micronaut.samples.petclinic.dto.OwnerLoyaltySummary;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.mapper.FormMapper;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.samples.petclinic.service.OwnerLoyaltyService;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import java.net.URI;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import java.util.Set;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for owner-related operations.
 * Handles CRUD operations and searching for pet owners.
 */
@Controller("/owners")
@Secured(SecurityRule.IS_ANONYMOUS)
public class OwnerController {

    private final ClinicService clinicService;
    private final FormMapper formMapper;
    private final OwnerLoyaltyService ownerLoyaltyService;

    /**
     * Creates the controller with its service and mapper dependencies.
     *
     * @param clinicService the facade used for owner operations
     * @param formMapper the mapper used to convert owner forms
     * @param ownerLoyaltyService profile-specific loyalty account and summary operations
     */
    public OwnerController(ClinicService clinicService, FormMapper formMapper,
                           OwnerLoyaltyService ownerLoyaltyService) {
        this.clinicService = clinicService;
        this.formMapper = formMapper;
        this.ownerLoyaltyService = ownerLoyaltyService;
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
        return Map.of(
                "owner", new Owner(),
                "notFound", notFound
        );
    }

    /**
     * Searches owners and returns matching owner card data and the total count.
     * Redirects to the search form when there are no matches, or to owner details
     * when the only match is present on the requested page.
     *
     * @param lastName case-insensitive last-name fragment; empty matches all owners
     * @param page zero-based page index, defaulting to 0
     * @param size owners per page, from 1 to 100; defaults to 5
     * @param sort sort field: {@code lastName} (default), {@code firstName}, {@code city}, or {@code id}
     * @param direction case-insensitive {@code asc} or {@code desc}; defaults to {@code asc}
     * @return a redirect or owner card data with the total matching count
     * @throws IllegalRequestArgumentException if pagination or ordering parameters are invalid
     */
    @Get
    public HttpResponse<?> processFindForm(@QueryValue(defaultValue = "") String lastName,
            @QueryValue(defaultValue = "0") int page,
            @QueryValue(defaultValue = "5") int size,
            @QueryValue(defaultValue = "lastName") String sort,
            @QueryValue(defaultValue = "asc") String direction) throws IllegalRequestArgumentException {
        Page<Owner> results = clinicService.findOwners(lastName, ownerPageable(page, size, sort, direction));
        if (results.getTotalSize() == 0) {
            return HttpResponse.redirect(UriBuilder.of("/owners/find").queryParam("notFound", true).build());
        }
        if (results.getTotalSize() == 1 && !results.getContent().isEmpty()) {
            return HttpResponse.redirect(UriBuilder.of("/owners/{ownerId}")
                    .expand(Map.of("ownerId", results.getContent().getFirst().id())));
        }
        return HttpResponse.ok(ownerPageData(results));
    }

    /**
     * Renders the owner-list shell. JavaScript reads query parameters and fetches the page data.
     *
     * @return an empty view model; list state and presentation are owned by the frontend
     */
    @Get("/list")
    @View("owners/ownersList")
    public Map<String, Object> showOwnerList() {
        return Map.of();
    }

    /**
     * Returns owner card data and its total matching count for asynchronous updates.
     * Always returns JSON, including when the result contains zero or one owner.
     *
     * @param lastName case-insensitive last-name fragment; empty matches all owners
     * @param page zero-based page index, defaulting to 0
     * @param size owners per page, from 1 to 100; defaults to 5
     * @param sort sort field: {@code lastName} (default), {@code firstName}, {@code city}, or {@code id}
     * @param direction case-insensitive {@code asc} or {@code desc}; defaults to {@code asc}
     * @return owner card data and the total matching count
     * @throws IllegalRequestArgumentException if pagination or ordering parameters are invalid
     */
    @Get(value = "/data", produces = MediaType.APPLICATION_JSON)
    public Map<String, Object> ownerData(@QueryValue(defaultValue = "") String lastName,
            @QueryValue(defaultValue = "0") int page,
            @QueryValue(defaultValue = "5") int size,
            @QueryValue(defaultValue = "lastName") String sort,
            @QueryValue(defaultValue = "asc") String direction) throws IllegalRequestArgumentException {
        Page<Owner> owners = clinicService.findOwners(lastName, ownerPageable(page, size, sort, direction));
        return ownerPageData(owners);
    }

    /**
     * Validates paging inputs and adds ascending owner ID as a stable tie-breaker
     * when the primary sort field is not already the ID.
     *
     * @param page the zero-based page index
     * @param size the number of owners per page, from 1 to 100
     * @param sort the allowed owner property to sort by
     * @param direction ascending or descending ordering, ignoring case
     * @return the validated page request with deterministic ordering
     * @throws IllegalRequestArgumentException if any input is invalid
     */
    private Pageable ownerPageable(int page, int size, String sort, String direction) throws IllegalRequestArgumentException {
        if (page < 0 || size < 1 || size > 100
                || !Set.of("id", "firstName", "lastName", "city").contains(sort)
                || !(direction.equalsIgnoreCase("asc") || direction.equalsIgnoreCase("desc"))) {
            throw new IllegalRequestArgumentException("Use page >= 0, size 1–100, sort id/firstName/lastName/city, direction asc/desc");
        }
        Sort.Order order = direction.equalsIgnoreCase("asc") ? Sort.Order.asc(sort) : Sort.Order.desc(sort);
        Sort ordering = sort.equals("id") ? Sort.of(order) : Sort.of(order, Sort.Order.asc("id"));
        return Pageable.from(page, size, ordering);
    }

    /**
     * Projects a database page into owner card data without presentation state or nested associations.
     *
     * @param owners the requested database page
     * @return the owner items and total count needed by the frontend
     */
    private Map<String, Object> ownerPageData(Page<Owner> owners) {
        return Map.of("owners", owners.getContent().stream().map(owner -> new OwnerListItem(
                owner.id(), owner.firstName(), owner.lastName(), owner.city(), owner.address(),
                owner.telephone(), owner.getPets().stream().map(Pet::name).toList())).toList(),
                "totalSize", owners.getTotalSize());
    }

    /**
     * Display owner details.
     *
     * @param ownerId the owner ID
     * @return the owner details view
     * @throws NotFoundException if the owner does not exist
     */
    @Get("/{ownerId}")
    @View("owners/ownerDetails")
    public Map<String, Object> showOwner(@PathVariable Integer ownerId) {
        Owner owner;
        try {
            owner = clinicService.findOwnerById(ownerId);
        } catch (NotFoundEntityException e) {
            throw new NotFoundException();
        }
        OwnerLoyaltySummary loyalty = null;
        if (ownerLoyaltyService.isEnabled()) {
            try {
                loyalty = ownerLoyaltyService.summary(ownerLoyaltyService.findOwnerLoyaltyAccount(owner));
            } catch (NotFoundEntityException ignored) {
                // A missing loyalty account hides the card, not the owner's details page.
            }
        }
        Map<String, Object> model = new HashMap<>();
        model.put("owner", owner);
        model.put("loyalty", loyalty);
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
    @Error(exception = ConstraintViolationException.class)
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
    @Get("/{ownerId}/edit")
    @View("owners/createOrUpdateOwnerForm")
    public Map<String, Object> initUpdateOwnerForm(@PathVariable Integer ownerId) {
        Owner owner;
        try {
            owner = clinicService.findOwnerById(ownerId);
        } catch (NotFoundEntityException e) {
            throw new NotFoundException();
        }
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
     * @throws NotFoundEntityException if the owner does not exist
     */
    @Post(value = "/{ownerId}/edit", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processUpdateOwnerForm(@PathVariable Integer ownerId, @Valid @Body OwnerForm form)
            throws NotFoundEntityException {
        Owner existingOwner = clinicService.findOwnerById(ownerId);

        Owner owner = formMapper.updateOwner(existingOwner, form);
        clinicService.saveOwner(owner);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        return HttpResponse.redirect(uri);
    }
}
