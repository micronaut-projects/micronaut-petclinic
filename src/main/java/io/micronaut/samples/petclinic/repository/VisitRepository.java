package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Visit;
import java.util.Collection;

/**
 * Repository for {@link Visit} entities.
 * Uses Micronaut Data JDBC for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface VisitRepository extends CrudRepository<Visit, Integer> {

    /**
     * Find all visits for a specific pet.
     * @param petId the pet ID
     * @return collection of visits for the pet
     */
    Collection<Visit> findByPetId(Integer petId);
}
