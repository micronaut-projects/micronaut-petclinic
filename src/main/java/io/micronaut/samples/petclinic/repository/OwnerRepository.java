package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.samples.petclinic.model.Owner;
import java.util.Collection;
import java.util.Optional;

/**
 * Repository for {@link Owner} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 */
@Repository
public interface OwnerRepository extends JpaRepository<Owner, Integer> {

    /**
     * Find owners by last name, using a case-insensitive LIKE search.
     * @param lastName the last name to search for
     * @return collection of matching owners
     */
    @Query("SELECT DISTINCT o FROM Owner o LEFT JOIN FETCH o.pets WHERE LOWER(o.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))")
    Collection<Owner> findByLastName(String lastName);

    /**
     * Find an owner by ID, eagerly fetching pets.
     * @param id the owner ID
     * @return the owner with pets loaded
     */
    @Query("SELECT o FROM Owner o LEFT JOIN FETCH o.pets WHERE o.id = :id")
    Optional<Owner> findByIdWithPets(Integer id);

    /**
     * Find all owners, ordered by last name.
     * @return collection of all owners
     */
    @Query("SELECT DISTINCT o FROM Owner o LEFT JOIN FETCH o.pets ORDER BY o.lastName")
    Collection<Owner> findAllWithPets();
}
