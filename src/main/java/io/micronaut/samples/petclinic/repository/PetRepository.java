package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Pet;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.micronaut.data.annotation.Join.Type.LEFT_FETCH;

/**
 * Repository for {@link Pet} entities.
 * Uses Micronaut Data JDBC for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface PetRepository extends CrudRepository<Pet, Integer> {
    /**
     * Locks the pet row until the caller's transaction completes.
     *
     * @param id the pet identifier
     * @return the locked identifier, or an empty result if the pet does not exist
     */
    @Query("SELECT ID FROM PETS WHERE ID = :id FOR UPDATE")
    Optional<Integer> lockById(Integer id);

    /**
     * Reloads the pet and its associations after acquiring its row lock.
     * Must be called within a transaction so the lock remains held during the subsequent save.
     *
     * @param id the pet identifier
     * @return the locked pet, or an empty result if it does not exist
     */
    default Optional<Pet> findByIdForUpdate(Integer id) {
        return lockById(id).flatMap(lockedId -> findById(lockedId));
    }

    /**
     * Find all pets.
     * @return collection of pets.
     */
    @Join(value = "type", type = LEFT_FETCH)
    List<Pet> findAll();

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
     * Finds a pet belonging to the required owner, with type and visits loaded.
     *
     * @param petId the pet identifier
     * @param ownerId the identifier of the required owner
     * @return the pet, or an empty result if the pet does not belong to the owner
     */
    @Join(value = "owner", type = LEFT_FETCH)
    @Join(value = "type", type = LEFT_FETCH)
    @Join(value = "visits", type = LEFT_FETCH)
    Optional<Pet> findByIdAndOwnerId(Integer petId, Integer ownerId);

    /**
     * Finds and locks a pet belonging to the required owner within the caller's transaction.
     *
     * @param id the pet identifier
     * @param ownerId the required owner identifier
     * @return the locked pet, if found
     */
    @Join(value = "owner", type = LEFT_FETCH)
    @Join(value = "type", type = LEFT_FETCH)
    @Join(value = "visits", type = LEFT_FETCH)
    Optional<Pet> findByIdAndOwnerIdForUpdate(Integer id, Integer ownerId);

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
