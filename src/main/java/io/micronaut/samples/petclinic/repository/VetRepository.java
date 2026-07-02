package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.model.VetWithSpecialities;
import java.util.Collection;

/**
 * Repository for {@link Vet} entities.
 * Uses Micronaut Data JDBC for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface VetRepository extends CrudRepository<Vet, Integer> {

    /**
     * Find all vets with their aggregated specialities, ordered by last name.
     * @return collection of all vets with specialities loaded
     */
    Collection<VetWithSpecialities> findAllWithSpecialities();
}
