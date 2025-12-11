package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import java.util.Collection;
import java.util.Optional;

/**
 * Repository for {@link Pet} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 */
@Repository
public interface PetRepository extends JpaRepository<Pet, Integer> {

    /**
     * Find all pet types, ordered by name.
     * @return collection of all pet types
     */
    @Query("SELECT pt FROM PetType pt ORDER BY pt.name")
    Collection<PetType> findPetTypes();

    /**
     * Find a pet by ID, eagerly fetching visits.
     * @param id the pet ID
     * @return the pet with visits loaded
     */
    @Query("SELECT p FROM Pet p LEFT JOIN FETCH p.visits WHERE p.id = :id")
    Optional<Pet> findByIdWithVisits(Integer id);

    /**
     * Find all pets for a specific owner.
     * @param ownerId the owner ID
     * @return collection of pets belonging to the owner
     */
    @Query("SELECT p FROM Pet p WHERE p.owner.id = :ownerId ORDER BY p.name")
    Collection<Pet> findByOwnerId(Integer ownerId);
}
