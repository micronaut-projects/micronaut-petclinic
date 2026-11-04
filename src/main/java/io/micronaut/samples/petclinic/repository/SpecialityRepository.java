package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Speciality;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Finds the existing specialities among the requested identifiers.
     *
     * @param ids requested speciality identifiers
     * @return matching specialities; unknown identifiers are omitted
     */
    Set<Speciality> findAllByIdIn(Set<Integer> ids);

    /**
     * Checks for a speciality with the supplied name.
     *
     * @param name name compared using the database collation
     * @return whether a matching speciality exists
     */
    boolean existsByName(String name);
}
