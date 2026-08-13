package io.micronaut.samples.petclinic.mapper;

import io.micronaut.context.annotation.Mapper;
import io.micronaut.context.annotation.Mapper.Mapping;
import io.micronaut.samples.petclinic.dto.OwnerForm;
import io.micronaut.samples.petclinic.dto.PetForm;
import io.micronaut.samples.petclinic.dto.VisitForm;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Visit;
import jakarta.inject.Singleton;

/**
 * Maps web form DTOs to and from the domain model.
 * <p>
 * The form types represent URL-encoded request data, while the model types are
 * immutable records used by the repository and service layers. Micronaut
 * generates the implementations for these methods from the {@link Mapper}
 * annotations, keeping controllers free of manual field-copying logic.
 */
@Mapper
public interface FormMapper {

    /**
     * Maps an owner form to a new owner entity.
     *
     * @param form the submitted owner form
     * @return the mapped owner entity
     */
    Owner toOwner(OwnerForm form);

    /**
     * Applies owner form values to an existing owner entity.
     *
     * @param owner the existing owner
     * @param form the submitted owner form
     * @return an updated owner copy
     */
    Owner updateOwner(Owner owner, OwnerForm form);

    /**
     * Maps an owner entity to a form DTO.
     *
     * @param owner the owner entity
     * @return the form DTO
     */
    OwnerForm toOwnerForm(Owner owner);

    /**
     * Maps a pet form to a new pet entity.
     *
     * @param form the submitted pet form
     * @return the mapped pet entity
     */
    Pet toPet(PetForm form);

    /**
     * Applies pet form values to an existing pet entity.
     *
     * @param pet the existing pet
     * @param form the submitted pet form
     * @return an updated pet copy
     */
    Pet updatePet(Pet pet, PetForm form);

    /**
     * Maps a pet entity to a form DTO.
     *
     * @param pet the pet entity
     * @return the form DTO
     */
    @Mapping(from = "#{pet.type.id}", to = "typeId")
    PetForm toPetForm(Pet pet);

    /**
     * Maps a visit form to a new visit entity.
     *
     * @param form the submitted visit form
     * @return the mapped visit entity
     */
    Visit toVisit(VisitForm form);
}
