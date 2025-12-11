package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.samples.petclinic.model.PetType;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link PetType} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 */
@Repository
public interface PetTypeRepository extends JpaRepository<PetType, Integer> {

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
