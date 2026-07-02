package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Speciality;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Speciality} entities.
 * Uses Micronaut Data JDBC for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface SpecialityRepository extends CrudRepository<Speciality, Integer> {

    /**
     * Find all specialities, ordered by name.
     * @return list of all specialities
     */
    List<Speciality> findAllOrderByName();

    /**
     * Find a speciality by name.
     * @param name the speciality name
     * @return the speciality, if found
     */
    Optional<Speciality> findByName(String name);
}
