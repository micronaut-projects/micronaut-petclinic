package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Join;
import static io.micronaut.data.annotation.Join.Type.LEFT_FETCH;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.data.model.Sort;
import java.util.Collection;
import java.util.List;
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
    @Join(value = "pets", type = LEFT_FETCH)
    @Join(value = "pets.type", type = LEFT_FETCH)
    Collection<Owner> findByLastNameIlike(String lastName, Sort sort);

    @Join(value = "pets", type = LEFT_FETCH)
    @Join(value = "pets.type", type = LEFT_FETCH)
    Collection<Owner> findByLastNameContainingIgnoreCase(String lastName, Sort sort);

    /**
     * Find an owner by ID, eagerly fetching pets.
     * @param id the owner ID
     * @return the owner with pets loaded
     */
    // Pets are loaded explicitly via PetRepository
    default Optional<Owner> findByIdWithPets(Integer id) {
        return findById(id);
    }

    @Join(value = "pets", type = LEFT_FETCH)
    @Join(value = "pets.type", type = LEFT_FETCH)
    List<Owner> findAll(Sort sort);

    @Join(value = "pets", type = LEFT_FETCH)
    @Join(value = "pets.type", type = LEFT_FETCH)
    @Join(value = "pets.visits", type = LEFT_FETCH)
    Optional<Owner> findById(Integer id);
}
