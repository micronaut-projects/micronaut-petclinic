package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.BasicHttpAttributes;
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
import io.micronaut.http.uri.UriMatchInfo;
import io.micronaut.http.uri.UriMatchTemplate;
import io.micronaut.samples.petclinic.dto.PetForm;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.mapper.FormMapper;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.ModelAndView;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for pet-related operations.
 * Handles CRUD operations for pets within the context of their owners.
 */
@Controller("/owners/{ownerId}/pets")
@Secured(SecurityRule.IS_ANONYMOUS)
public class PetController {

    private final ClinicService clinicService;
    private final FormMapper formMapper;

    /**
     * Creates the controller with its service and mapper dependencies.
     *
     * @param clinicService the facade used for pet operations
     * @param formMapper the mapper used to convert pet forms
     */
    public PetController(ClinicService clinicService, FormMapper formMapper) {
        this.clinicService = clinicService;
        this.formMapper = formMapper;
    }

    /**
     * Renders the pet form when validation fails.
     *
     * @param request the request containing the submitted form
     * @param e the validation exception
     * @return HTTP 400 with the submitted form, or HTTP 404 if the route or owner cannot be resolved
     */
    @io.micronaut.http.annotation.Error(exception = ConstraintViolationException.class)
    public HttpResponse<?> onConstraintViolation(HttpRequest<?> request,
                                                 ConstraintViolationException e) {
        Owner owner;
        Integer petId;
        try {
            // Error handling replaces the route match, but retains the original URI template.
            Map<String, Object> pathVariables = BasicHttpAttributes.getUriTemplate(request)
                    .flatMap(template -> UriMatchTemplate.of(template).match(request.getPath()))
                    .map(UriMatchInfo::getVariableValues)
                    .orElseThrow(NotFoundEntityException::new);
            Integer ownerId = Optional.ofNullable(pathVariables.get("ownerId"))
                    .map(Object::toString).map(Integer::valueOf).orElseThrow(NotFoundEntityException::new);
            petId = Optional.ofNullable(pathVariables.get("petId"))
                    .map(Object::toString).map(Integer::valueOf).orElse(null);
            owner = getOwner(ownerId);
        } catch (NotFoundEntityException notFound) {
            return HttpResponse.notFound(new ModelAndView<>("error/404", Map.of(
                    "path", request.getPath(), "message", notFound.getMessage())));
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

        // Re-populate the form fields from submitted values where possible.
        PetForm form = request.getBody(PetForm.class).orElseGet(PetForm::new);
        form = new PetForm(petId, form.name(), form.birthDate(), form.typeId());
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("owner", owner);
        model.put("types", clinicService.findPetTypes());
        model.put("isNew", petId == null);
        model.put("validationErrors", validationErrors);
        model.put("pet", form);
        if (petId != null) {
            model.put("petId", petId);
        }

        return HttpResponse.badRequest(new ModelAndView<>("pets/createOrUpdatePetForm", model));
    }

    /**
     * Get the owner for a pet operation.
     *
     * @param ownerId the owner ID
     * @return the persisted owner
     * @throws NotFoundEntityException if the owner does not exist
     */
    private Owner getOwner(Integer ownerId) throws NotFoundEntityException {
        return clinicService.findOwnerById(ownerId);
    }

    /**
     * Display the form to create a new pet.
     *
     * @param ownerId the owner ID
     * @return the create pet form view
     */
    @Get("/new")
    @View("pets/createOrUpdatePetForm")
    public Map<String, Object> initCreationForm(@PathVariable Integer ownerId) {
        Owner owner;
        try {
            owner = getOwner(ownerId);
        } catch (NotFoundEntityException e) {
            throw new NotFoundException();
        }
        return Map.of("pet", new PetForm(),
                "owner", owner,
                "types", clinicService.findPetTypes(),
                "isNew", true,
                "validationErrors", Map.of()
        );
    }

    /**
     * Process the form to create a new pet.
     *
     * @param ownerId the owner ID
     * @param form    the pet form data
     * @return redirect to owner details
     */
    @Post(value = "/new", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processCreationForm(@PathVariable Integer ownerId, @Valid @Body PetForm form)
            throws NotFoundEntityException {
        Owner owner = getOwner(ownerId);

        Pet pet = formMapper.toPet(form).withOwner(owner);

        // Set pet type - validation ensures typeId is not null
        if (form.typeId() != null) {
            Optional<PetType> petType = clinicService.findPetTypeById(form.typeId());
            if (petType.isEmpty()) {
                return HttpResponse.badRequest("Invalid pet type");
            }
            pet = pet.withType(petType.get());
        }

        clinicService.savePet(pet);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        return HttpResponse.redirect(uri);
    }

    /**
     * Display the form to edit an existing pet.
     *
     * @param ownerId the owner ID
     * @param petId   the pet ID
     * @return the edit pet form view
     */
    @Get("/{petId}/edit")
    @View("pets/createOrUpdatePetForm")
    public Map<String, Object> initUpdateForm(@PathVariable Integer ownerId, @PathVariable Integer petId) {
        Pet pet;
        try {
            pet = clinicService.findPetById(petId);
        } catch (NotFoundEntityException e) {
            throw new NotFoundException();
        }
        return Map.of("pet", formMapper.toPetForm(pet),
                "petId", petId,
                "owner", pet.getOwner(),
                "types", clinicService.findPetTypes(),
                "isNew", false,
                "validationErrors", Map.of()
        );
    }

    /**
     * Process the form to update an existing pet.
     *
     * @param ownerId the owner ID
     * @param petId   the pet ID
     * @param form    the updated pet form data
     * @return redirect to owner details
     */
    @Post(value = "/{petId}/edit", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processUpdateForm(@PathVariable Integer ownerId,
                                             @PathVariable Integer petId,
                                             @Valid @Body PetForm form) throws NotFoundEntityException {
        Owner owner = getOwner(ownerId);
        Pet existingPet = clinicService.findPetById(petId);

        Pet pet = formMapper.updatePet(existingPet, form).withOwner(owner);

        // Set pet type - validation ensures typeId is not null
        if (form.typeId() != null) {
            Optional<PetType> petType = clinicService.findPetTypeById(form.typeId());
            if (petType.isEmpty()) {
                return HttpResponse.badRequest("Invalid pet type");
            }
            pet = pet.withType(petType.get());
        }

        clinicService.savePet(pet);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        return HttpResponse.redirect(uri);
    }
}
