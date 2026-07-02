package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.PetType;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link PetType} entities.
 * Uses Micronaut Data JDBC for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface PetTypeRepository extends CrudRepository<PetType, Integer> {

    /**
     * Find all pet types, ordered by name.
     * @return list of all pet types
     */
    List<PetType> findAllOrderByName();

    /**
     * Find a pet type by name.
     * @param name the type name
     * @return the pet type, if found
     */
    Optional<PetType> findByName(String name);
}
