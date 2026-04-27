package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Vet;
import java.util.Collection;

/**
 * Repository for {@link Vet} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 */
@JdbcRepository(dialect = Dialect.ANSI)
public interface VetRepository extends CrudRepository<Vet, Integer> {

    /**
     * Find all vets with their specialties, ordered by last name.
     * @return collection of all vets with specialties loaded
     */
    @Query("SELECT v.* FROM vets v ORDER BY v.last_name")
    Collection<Vet> findAllWithSpecialties();
}
