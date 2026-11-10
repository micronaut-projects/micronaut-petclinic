package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.BasicHttpAttributes;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Part;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
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
import io.micronaut.samples.petclinic.service.InvalidPetPhotoException;
import io.micronaut.samples.petclinic.service.PetPhotoStorage;
import io.micronaut.samples.petclinic.service.PetPhotoValidator;
import io.micronaut.samples.petclinic.service.PetPhotoTooLargeException;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.ModelAndView;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    private static final Logger LOG = LoggerFactory.getLogger(PetController.class);

    private final ClinicService clinicService;
    private final FormMapper formMapper;
    private final PetPhotoStorage petPhotoStorage;
    private final PetPhotoValidator petPhotoValidator;

    /**
     * Creates the controller with its service and mapper dependencies.
     *
     * @param clinicService the facade used for pet operations
     * @param formMapper the mapper used to convert pet forms
     */
    public PetController(ClinicService clinicService, FormMapper formMapper, PetPhotoStorage petPhotoStorage, PetPhotoValidator petPhotoValidator) {
        this.clinicService = clinicService;
        this.formMapper = formMapper;
        this.petPhotoStorage = petPhotoStorage;
        this.petPhotoValidator = petPhotoValidator;
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
        String photoUrl = "";
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
            if (petId != null) {
                Pet existing = clinicService.findPetById(petId);
                if (!ownerId.equals(existing.getOwnerId())) {
                    throw new NotFoundEntityException();
                }
                if (existing.photoFilename() != null) {
                    photoUrl = "/owners/" + ownerId + "/pets/" + petId + "/photo";
                }
            }
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
        model.put("photoError", "");
        model.put("photoUrl", photoUrl);
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
                "validationErrors", Map.of(),
                "photoError", "",
                "photoUrl", ""
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
    public Map<String, Object> initUpdateForm(@PathVariable Integer ownerId,
                                             @PathVariable Integer petId,
                                             @io.micronaut.http.annotation.QueryValue(defaultValue = "") String photoError) {
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
                "validationErrors", Map.of(),
                "photoError", photoError,
                "photoUrl", pet.photoFilename() != null
                        ? "/owners/" + ownerId + "/pets/" + petId + "/photo" : ""
        );
    }

    /**
     * Upload or replace a pet photo. Only generated filenames are persisted and served.
     *
     * @param ownerId the owner in the route
     * @param petId the pet receiving the photo
     * @param upload the multipart image
     * @return redirect to the owner details page, or a validation response
     */
    @Post(value = "/{petId}/photo", consumes = MediaType.MULTIPART_FORM_DATA)
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<?> uploadPhoto(@PathVariable Integer ownerId,
                                       @PathVariable Integer petId,
                                       @Part("photo") CompletedFileUpload upload) {
        Pet pet;
        try {
            pet = clinicService.findPetById(petId);
        } catch (NotFoundEntityException e) {
            throw new NotFoundException();
        }
        if (!ownerId.equals(pet.getOwnerId())) {
            throw new NotFoundException();
        }

        String previousFilename = pet.photoFilename();
        String newFilename;
        try (upload) {
            newFilename = petPhotoStorage.store(petPhotoValidator.validate(upload));
        } catch (InvalidPetPhotoException invalidPhoto) {
            return HttpResponse.redirect(URI.create("/owners/" + ownerId + "/pets/" + petId
                    + "/edit?photoError=invalid-type"));
        } catch (PetPhotoTooLargeException tooLarge) {
            return HttpResponse.status(HttpStatus.REQUEST_ENTITY_TOO_LARGE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to store the pet photo", e);
        }

        try {
            if (!clinicService.replacePetPhoto(petId, previousFilename, newFilename)) {
                petPhotoStorage.delete(newFilename);
                return HttpResponse.status(HttpStatus.CONFLICT);
            }
        } catch (RuntimeException e) {
            try {
                petPhotoStorage.delete(newFilename);
            } catch (IOException cleanupFailure) {
                e.addSuppressed(cleanupFailure);
            }
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to clean up the unreferenced pet photo", e);
        }

        if (previousFilename != null && !previousFilename.equals(newFilename)) {
            try {
                petPhotoStorage.delete(previousFilename);
            } catch (IOException cleanupFailure) {
                LOG.warn("Could not delete replaced pet photo {}", previousFilename, cleanupFailure);
            }
        }
        return HttpResponse.redirect(UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId)));
    }

    /**
     * Serves the stored photo for the pet without accepting a filesystem path from the request.
     *
     * @param ownerId the owner in the route
     * @param petId the pet whose photo is requested
     * @return the verified image, or not found
     * @throws IOException if the image cannot be read
     */
    @Get("/{petId}/photo")
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<byte[]> showPhoto(@PathVariable Integer ownerId, @PathVariable Integer petId)
            throws IOException {
        Pet pet;
        try {
            pet = clinicService.findPetById(petId);
        } catch (NotFoundEntityException e) {
            throw new NotFoundException();
        }
        if (!ownerId.equals(pet.getOwnerId())) {
            throw new NotFoundException();
        }
        PetPhotoStorage.StoredPhoto photo = petPhotoStorage.load(pet.photoFilename())
                .orElseThrow(NotFoundException::new);
        return HttpResponse.ok(photo.bytes())
                .contentType(photo.mediaType())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff");
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

        clinicService.updatePetDetails(pet);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        return HttpResponse.redirect(uri);
    }
}
