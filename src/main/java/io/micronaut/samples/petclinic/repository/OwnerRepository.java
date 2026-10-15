package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Join;
import static io.micronaut.data.annotation.Join.Type.LEFT_FETCH;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.data.model.Sort;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Owner} entities.
 * Uses Micronaut Data JDBC for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface OwnerRepository extends CrudRepository<Owner, Integer> {

    /**
     * Find owners by last name, using a case-insensitive LIKE search.
     * @param lastName the last name to search for
     * @param sort the sort order for results
     * @return collection of matching owners
     */
    @Join(value = "pets", type = LEFT_FETCH)
    @Join(value = "pets.type", type = LEFT_FETCH)
    Collection<Owner> findByLastNameIlike(String lastName, Sort sort);

    /**
     * Finds owners by case-insensitive partial last-name match.
     *
     * @param lastName the last-name fragment to search for
     * @param sort the sort order for results
     * @return matching owners with pets and pet types loaded
     */
    @Join(value = "pets", type = LEFT_FETCH)
    @Join(value = "pets.type", type = LEFT_FETCH)
    Collection<Owner> findByLastNameContainsIgnoreCase(String lastName, Sort sort);

    /**
     * Finds owners by case-insensitive partial last-name match.
     *
     * @param lastName the last-name fragment to search for
     * @return matching owners with pets and pet types loaded
     */
    @Join(value = "pets", type = LEFT_FETCH)
    @Join(value = "pets.type", type = LEFT_FETCH)
    Collection<Owner> findByLastNameContainsIgnoreCase(String lastName);

    /**
     * Find an owner by ID, eagerly fetching pets.
     * Pets are loaded explicitly via {@link PetRepository}.
     *
     * @param id the owner ID
     * @return the owner with pets loaded
     */
    default Optional<Owner> findByIdWithPets(Integer id) {
        return findById(id);
    }

    /**
     * Finds all owners using the supplied sort order.
     *
     * @param sort the sort order for results
     * @return all owners with pets and pet types loaded
     */
    @Join(value = "pets", type = LEFT_FETCH)
    @Join(value = "pets.type", type = LEFT_FETCH)
    List<Owner> findAll(Sort sort);

    /**
     * Finds an owner by id with pets, pet types, and visits loaded.
     *
     * @param id the owner id
     * @return the owner, if found
     */
    @Join(value = "pets", type = LEFT_FETCH)
    @Join(value = "pets.type", type = LEFT_FETCH)
    @Join(value = "pets.visits", type = LEFT_FETCH)
    Optional<Owner> findById(Integer id);

    /**
     * Finds owners by lastName.
     *
     * @param id the owner id
     * @return the owner, if found
     */
    Collection<Owner> findByLastName(String lastName);
}
