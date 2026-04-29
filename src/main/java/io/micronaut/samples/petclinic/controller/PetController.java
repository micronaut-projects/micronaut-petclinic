package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.samples.petclinic.dto.PetForm;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.views.View;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.*;

/**
 * Controller for pet-related operations.
 * Handles CRUD operations for pets within the context of their owners.
 */
@Controller("/owners/{ownerId}/pets")
public class PetController {

    private final ClinicService clinicService;

    public PetController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    /**
     * Get the owner for a pet operation.
     *
     * @param ownerId the owner ID
     * @return the owner, or empty if not found
     */
    private Optional<Owner> getOwner(Integer ownerId) {
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
        Map<String, Object> model = new HashMap<>();
        Optional<Owner> owner = getOwner(ownerId);

        if (owner.isPresent()) {
            model.put("pet", new PetForm());
            model.put("owner", owner.get());
            model.put("types", clinicService.findPetTypes());
            model.put("isNew", true);
        } else {
            model.put("error", "Owner not found");
        }
        return model;
    }

    /**
     * Process the form to create a new pet.
     *
     * @param ownerId the owner ID
     * @param form    the pet form data
     * @return redirect to owner details
     */
    @Post(value = "/new", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processCreationForm(@PathVariable Integer ownerId, @Valid @Body PetForm form) {
        Optional<Owner> owner = getOwner(ownerId);

        if (owner.isEmpty()) {
            return HttpResponse.notFound();
        }

        Pet pet = form.toPet();

        // Set pet type - validation ensures typeId is not null
        if (form.getTypeId() != null) {
            Optional<PetType> petType = clinicService.findPetTypeById(form.getTypeId());
            if (petType.isEmpty()) {
                return HttpResponse.badRequest("Invalid pet type");
            }
            pet.setType(petType.get());
        }

        owner.get().addPet(pet);
        clinicService.savePet(pet);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        // 303 makes sure the browser follows up with GET.
        return HttpResponse.seeOther(uri);
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
        Map<String, Object> model = new HashMap<>();
        Optional<Pet> pet = clinicService.findPetById(petId);

        if (pet.isPresent()) {
            model.put("pet", PetForm.fromPet(pet.get()));
            model.put("petId", petId);
            model.put("owner", pet.get().getOwner());
            model.put("types", clinicService.findPetTypes());
            model.put("isNew", false);
        } else {
            model.put("error", "Pet not found");
        }
        return model;
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
                                              @Valid @Body PetForm form) {
        Optional<Owner> owner = getOwner(ownerId);
        if (owner.isEmpty()) {
            return HttpResponse.notFound();
        }

        Optional<Pet> existingPet = clinicService.findPetById(petId);
        if (existingPet.isEmpty()) {
            return HttpResponse.notFound();
        }

        Pet pet = form.updatePet(existingPet.get());

        // Set pet type - validation ensures typeId is not null
        if (form.getTypeId() != null) {
            Optional<PetType> petType = clinicService.findPetTypeById(form.getTypeId());
            if (petType.isEmpty()) {
                return HttpResponse.badRequest("Invalid pet type");
            }
            pet.setType(petType.get());
        }

        clinicService.savePet(pet);

        URI uri = UriBuilder.of("/owners/{ownerId}").expand(Map.of("ownerId", ownerId));
        return HttpResponse.seeOther(uri);
    }
}
