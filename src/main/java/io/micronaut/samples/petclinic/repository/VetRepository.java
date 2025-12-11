package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.samples.petclinic.model.Vet;
import java.util.Collection;

/**
 * Repository for {@link Vet} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 */
@Repository
public interface VetRepository extends JpaRepository<Vet, Integer> {

    /**
     * Find all vets with their specialties, ordered by last name.
     * @return collection of all vets with specialties loaded
     */
    @Query("SELECT DISTINCT v FROM Vet v LEFT JOIN FETCH v.specialties ORDER BY v.lastName")
    Collection<Vet> findAllWithSpecialties();
}
