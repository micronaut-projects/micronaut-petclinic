package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Specialty;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Specialty} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 */
@JdbcRepository(dialect = Dialect.ANSI)
public interface SpecialtyRepository extends CrudRepository<Specialty, Integer> {

    /**
     * Find all specialties, ordered by name.
     * @return list of all specialties
     */
    List<Specialty> findAllOrderByName();

    /**
     * Find a specialty by name.
     * @param name the specialty name
     * @return the specialty, if found
     */
    Optional<Specialty> findByName(String name);
}
