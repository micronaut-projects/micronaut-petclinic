package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.samples.petclinic.model.Visit;
import java.util.Collection;

/**
 * Repository for {@link Visit} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 */
@Repository
public interface VisitRepository extends JpaRepository<Visit, Integer> {

    /**
     * Find all visits for a specific pet.
     * @param petId the pet ID
     * @return collection of visits for the pet
     */
    @Query("SELECT v FROM Visit v WHERE v.pet.id = :petId ORDER BY v.date DESC")
    Collection<Visit> findByPetId(Integer petId);

    /**
     * Find all visits for a specific owner (across all their pets).
     * @param ownerId the owner ID
     * @return collection of visits for all pets of the owner
     */
    @Query("SELECT v FROM Visit v WHERE v.pet.owner.id = :ownerId ORDER BY v.date DESC")
    Collection<Visit> findByOwnerId(Integer ownerId);
}
