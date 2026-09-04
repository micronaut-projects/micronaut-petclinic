package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Join;
import static io.micronaut.data.annotation.Join.Type.LEFT_FETCH;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Pet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Pet} entities.
 * Uses Micronaut Data JDBC for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface PetRepository extends CrudRepository<Pet, Integer> {

    /**
     * Find a pet by ID, eagerly fetching visits.
     * Visits are loaded explicitly via {@link VisitRepository}.
     *
     * @param id the pet ID
     * @return the pet with visits loaded
     */
    default Optional<Pet> findByIdWithVisits(Integer id) {
        return findById(id);
    }

    /**
     * Finds a pet by id with owner, type, and visits loaded.
     *
     * @param id the pet id
     * @return the pet, if found
     */
    @Join(value = "owner", type = LEFT_FETCH)
    @Join(value = "type", type = LEFT_FETCH)
    @Join(value = "visits", type = LEFT_FETCH)
    Optional<Pet> findById(Integer id);

    /**
     * Find all pets for a specific owner.
     * @param ownerId the owner ID
     * @return collection of pets belonging to the owner
     */
    Collection<Pet> findByOwnerId(Integer ownerId);

    /**
     * Finds pets whose names contain the supplied text.
     *
     * @param name the pet name fragment
     * @return matching pets
     */
    @Join(value = "owner", type = LEFT_FETCH)
    @Join(value = "type", type = LEFT_FETCH)
    Collection<Pet> findByNameContainingIgnoreCase(String name);

    /**
     * Finds all pets for a collection of owners.
     *
     * @param ownerIds owner ids to match
     * @return pets belonging to the supplied owners
     */
    List<Pet> findByOwnerIdIn(List<Integer> ownerIds);
}
