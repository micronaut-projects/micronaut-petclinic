package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Vet;
import java.util.Collection;

/**
 * Repository for {@link Vet} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface VetRepository extends CrudRepository<Vet, Integer> {

    /**
     * Find all vets with their specialties, ordered by last name.
     * @return collection of all vets with specialties loaded
     */
    Collection<Vet> findAllWithSpecialties();
}
