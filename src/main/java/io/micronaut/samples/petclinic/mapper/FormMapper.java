package io.micronaut.samples.petclinic.mapper;

import io.micronaut.context.annotation.Mapper;
import io.micronaut.context.annotation.Mapper.Mapping;
import io.micronaut.samples.petclinic.dto.OwnerForm;
import io.micronaut.samples.petclinic.dto.PetForm;
import io.micronaut.samples.petclinic.dto.SpecialityDto;
import io.micronaut.samples.petclinic.dto.VetDto;
import io.micronaut.samples.petclinic.dto.VisitForm;
import io.micronaut.samples.petclinic.generated.OwnerCareSummary;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.model.Visit;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps web form and response DTOs to and from the domain model.
 * <p>
 * The form types represent URL-encoded request data, while the model types are
 * immutable records used by the repository and service layers. Micronaut
 * generates the simple mappings from the {@link Mapper} annotations; default
 * methods assemble nested response collections.
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
    default Visit toVisit(VisitForm form) {
        return new Visit(
                form.date(),
                form.description(),
                null,
                Duration.ofMinutes(form.durationMinutes()),
                Period.ofMonths(form.followUpPeriodMonths())
        );
    }

    /**
     * Maps a fetched veterinarian.
     *
     * @param vet source veterinarian
     * @return the veterinarian response DTO
     */
    default VetDto toVetDto(Vet vet) {
        return new VetDto(vet.id(), vet.firstName(), vet.lastName(),
                vet.specialities().stream().map(this::toSpecialityDto).collect(Collectors.toSet()), vet.etag());
    }

    /**
     * Maps a fetched owner and their care history to the generated export DTO.
     *
     * @param owner the persisted owner with pets and visits loaded
     * @return the generated owner care summary
     */
    @Mapping(from = "id", to = "_id")
    @Mapping(from = "#{this.toCareSummaryPets(owner)}", to = "pets")
    OwnerCareSummary toOwnerCareSummary(Owner owner);

    /**
     * Maps a fetched pet and visits, retaining their calendar dates.
     *
     * @param pet the persisted pet with visits loaded
     * @return the generated pet summary
     */
    @Mapping(from = "#{this.toCareSummaryVisits(pet)}", to = "visits")
    OwnerCareSummary.Pets toCareSummaryPet(Pet pet);

    /**
     * Maps a visit to the generated export DTO.
     *
     * @param visit the persisted visit
     * @return the generated visit summary
     */
    OwnerCareSummary.Pets.Visits toCareSummaryVisit(Visit visit);

    /**
     * Maps each pet because Micronaut's mapper copies assignable lists without converting their elements.
     *
     * @param owner the owner with pets and visits loaded
     * @return the immutable list of generated pet summaries
     */
    default List<OwnerCareSummary.Pets> toCareSummaryPets(Owner owner) {
        return owner.pets().stream().map(this::toCareSummaryPet).toList();
    }

    /**
     * Maps each visit to the generated export DTO.
     *
     * @param pet the pet with visits loaded
     * @return the immutable list of generated visit summaries
     */
    default List<OwnerCareSummary.Pets.Visits> toCareSummaryVisits(Pet pet) {
        return pet.visits().stream().map(this::toCareSummaryVisit).toList();
    }

    /**
     * Maps a speciality to specialityDto.
     *
     * @param speciality source speciality
     * @return the speciality response DTO
     */
    SpecialityDto toSpecialityDto(Speciality speciality);
}
