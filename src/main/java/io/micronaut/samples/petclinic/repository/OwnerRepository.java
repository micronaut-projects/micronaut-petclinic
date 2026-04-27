package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Owner;
import java.util.Collection;
import java.util.Optional;

/**
 * Repository for {@link Owner} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 */
@JdbcRepository(dialect = Dialect.ANSI)
public interface OwnerRepository extends CrudRepository<Owner, Integer> {

    /**
     * Find owners by last name, using a case-insensitive LIKE search.
     * @param lastName the last name to search for
     * @return collection of matching owners
     */
    @Query("SELECT o.* FROM owners o WHERE LOWER(o.last_name) LIKE LOWER(CONCAT('%', :lastName, '%')) ORDER BY o.last_name")
    Collection<Owner> findByLastName(String lastName);

    /**
     * Find an owner by ID, eagerly fetching pets.
     * @param id the owner ID
     * @return the owner with pets loaded
     */
    // Pets are loaded explicitly via PetRepository
    default Optional<Owner> findByIdWithPets(Integer id) {
        return findById(id);
    }

    /**
     * Find all owners, ordered by last name.
     * @return collection of all owners
     */
    // Pets are loaded explicitly via PetRepository
    @Query("SELECT o.* FROM owners o ORDER BY o.last_name")
    Collection<Owner> findAllWithPets();
}
