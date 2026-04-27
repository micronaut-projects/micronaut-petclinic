package io.micronaut.samples.petclinic.service;

import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.samples.petclinic.model.*;
import io.micronaut.samples.petclinic.repository.*;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service class providing business logic for the Pet Clinic application.
 * Acts as a facade over the repository layer.
 */
@Singleton
public class ClinicService {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final PetTypeRepository petTypeRepository;
    private final VisitRepository visitRepository;
    private final VetRepository vetRepository;
    private final SpecialtyRepository specialtyRepository;
    private final VetSpecialtyRepository vetSpecialtyRepository;

    public ClinicService(OwnerRepository ownerRepository,
                         PetRepository petRepository,
                          PetTypeRepository petTypeRepository,
                          VisitRepository visitRepository,
                          VetRepository vetRepository,
                          SpecialtyRepository specialtyRepository,
                          VetSpecialtyRepository vetSpecialtyRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
        this.petTypeRepository = petTypeRepository;
        this.visitRepository = visitRepository;
        this.vetRepository = vetRepository;
        this.specialtyRepository = specialtyRepository;
        this.vetSpecialtyRepository = vetSpecialtyRepository;
    }

    // ========== Owner Operations ==========

    /**
     * Find an owner by ID.
     * @param id the owner ID
     * @return the owner, if found
     */
    public Optional<Owner> findOwnerById(Integer id) {
        var ownerOpt = ownerRepository.findById(id);
        ownerOpt.ifPresent(owner -> hydrateOwnerPets(owner, true));
        return ownerOpt;
    }

    /**
     * Find owners by last name (case-insensitive partial match).
     * @param lastName the last name to search for
     * @return collection of matching owners
     */
    public Collection<Owner> findOwnerByLastName(String lastName) {
        var owners = ownerRepository.findByLastName(lastName);
        for (var owner : owners) {
            hydrateOwnerPets(owner, false);
        }
        return owners;
    }

    /**
     * Find all owners.
     * @return collection of all owners
     */
    public Collection<Owner> findAllOwners() {
        var owners = ownerRepository.findAllWithPets();
        for (var owner : owners) {
            hydrateOwnerPets(owner, false);
        }
        return owners;
    }

    /**
     * Save an owner (create or update).
     * @param owner the owner to save
     */
    @Transactional
    public Owner saveOwner(Owner owner) {
        if (owner.isNew()) {
            return ownerRepository.save(owner);
        } else {
            return ownerRepository.update(owner);
        }
    }

    /**
     * Delete an owner by ID.
     * @param id the owner ID
     */
    @Transactional
    public void deleteOwner(Integer id) {
        ownerRepository.deleteById(id);
    }

    // ========== Pet Operations ==========

    /**
     * Find a pet by ID.
     * @param id the pet ID
     * @return the pet, if found
     */
    public Optional<Pet> findPetById(Integer id) {
        var petOpt = petRepository.findById(id);
        petOpt.ifPresent(pet -> {
            hydratePetBasics(pet, null);
            pet.setVisits(new ArrayList<>(visitRepository.findByPetId(pet.getId())));
        });
        return petOpt;
    }

    /**
     * Find all pets.
     * @return list of all pets
     */
    public List<Pet> findAllPets() {
        return petRepository.findAll();
    }

    /**
     * Save a pet (create or update).
     * @param pet the pet to save
     */
    @Transactional
    public Pet savePet(Pet pet) {
        // In JDBC we persist foreign keys, not object references.
        if (pet.getTypeId() == null && pet.getType() != null) {
            pet.setTypeId(pet.getType().getId());
        }
        if (pet.getOwnerId() == null && pet.getOwner() != null) {
            pet.setOwnerId(pet.getOwner().getId());
        }
        if (pet.isNew()) {
            return petRepository.save(pet);
        } else {
            return petRepository.update(pet);
        }
    }

    /**
     * Delete a pet by ID.
     * @param id the pet ID
     */
    @Transactional
    public void deletePet(Integer id) {
        petRepository.deleteById(id);
    }

    // ========== Pet Type Operations ==========

    /**
     * Find all pet types.
     * @return list of all pet types
     */
    public List<PetType> findPetTypes() {
        return petTypeRepository.findAllOrderByName();
    }

    /**
     * Find a pet type by ID.
     * @param id the pet type ID
     * @return the pet type, if found
     */
    public Optional<PetType> findPetTypeById(Integer id) {
        return petTypeRepository.findById(id);
    }

    // ========== Visit Operations ==========

    /**
     * Find a visit by ID.
     * @param id the visit ID
     * @return the visit, if found
     */
    public Optional<Visit> findVisitById(Integer id) {
        return visitRepository.findById(id);
    }

    /**
     * Find all visits for a pet.
     * @param petId the pet ID
     * @return collection of visits
     */
    public Collection<Visit> findVisitsByPetId(Integer petId) {
        return visitRepository.findByPetId(petId);
    }

    /**
     * Save a visit (create or update).
     * @param visit the visit to save
     */
    @Transactional
    public Visit saveVisit(Visit visit) {
        if (visit.getPetId() == null && visit.getPet() != null) {
            visit.setPetId(visit.getPet().getId());
        }
        if (visit.isNew()) {
            return visitRepository.save(visit);
        } else {
            return visitRepository.update(visit);
        }
    }

    /**
     * Delete a visit by ID.
     * @param id the visit ID
     */
    @Transactional
    public void deleteVisit(Integer id) {
        visitRepository.deleteById(id);
    }

    // ========== Vet Operations ==========

    /**
     * Find all veterinarians.
     * Cached for performance.
     * @return collection of all vets
     */
    @Cacheable("vets")
    public Collection<Vet> findAllVets() {
        var vets = vetRepository.findAllWithSpecialties();
        for (var vet : vets) {
            vet.getSpecialtiesInternal().clear();
            vet.getSpecialtiesInternal().addAll(vetSpecialtyRepository.findSpecialtiesByVetId(vet.getId()));
        }
        return vets;
    }

    /**
     * Find a vet by ID.
     * @param id the vet ID
     * @return the vet, if found
     */
    public Optional<Vet> findVetById(Integer id) {
        return vetRepository.findById(id);
    }

    // ========== Specialty Operations ==========

    /**
     * Find all specialties.
     * @return list of all specialties
     */
    public List<Specialty> findAllSpecialties() {
        return specialtyRepository.findAllOrderByName();
    }

    /**
     * Find a specialty by ID.
     * @param id the specialty ID
     * @return the specialty, if found
     */
    public Optional<Specialty> findSpecialtyById(Integer id) {
        return specialtyRepository.findById(id);
    }

    private void hydrateOwnerPets(Owner owner, boolean includeVisits) {
        var pets = petRepository.findByOwnerId(owner.getId());
        for (var pet : pets) {
            hydratePetBasics(pet, owner);
            if (includeVisits) {
                pet.setVisits(new ArrayList<>(visitRepository.findByPetId(pet.getId())));
            }
        }
        owner.getPetsInternal().clear();
        owner.getPetsInternal().addAll(pets);
    }

    private void hydratePetBasics(Pet pet, Owner ownerOrNull) {
        if (ownerOrNull != null) {
            pet.setOwner(ownerOrNull);
        } else if (pet.getOwnerId() != null) {
            ownerRepository.findById(pet.getOwnerId()).ifPresent(pet::setOwner);
        }

        if (pet.getTypeId() != null) {
            petTypeRepository.findById(pet.getTypeId()).ifPresent(pet::setType);
        }
    }
}
