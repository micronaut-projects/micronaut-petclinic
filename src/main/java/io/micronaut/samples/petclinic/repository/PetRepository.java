package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Join;
import static io.micronaut.data.annotation.Join.Type.LEFT_FETCH;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Pet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Pet} entities.
 * Uses Micronaut Data JPA for compile-time query generation.
 */
@JdbcRepository(dialect = Dialect.ANSI)
public interface PetRepository extends CrudRepository<Pet, Integer> {

    /**
     * Find a pet by ID, eagerly fetching visits.
     * @param id the pet ID
     * @return the pet with visits loaded
     */
    // Visits are loaded explicitly via VisitRepository
    default Optional<Pet> findByIdWithVisits(Integer id) {
        return findById(id);
    }

    @Join(value = "owner", type = LEFT_FETCH)
    @Join(value = "type", type = LEFT_FETCH)
    @Join(value = "visits", type = LEFT_FETCH)
    Optional<Pet> findById(Integer id);

    /**
     * Find all pets for a specific owner.
     * @param ownerId the owner ID
     * @return collection of pets belonging to the owner
     */
    @Query("SELECT p.* FROM pets p WHERE p.owner_id = :ownerId ORDER BY p.name")
    Collection<Pet> findByOwnerId(Integer ownerId);

    @Query("SELECT p.* FROM pets p WHERE p.owner_id IN (:ownerIds) ORDER BY p.owner_id, p.name")
    List<Pet> findByOwnerIdIn(List<Integer> ownerIds);
}
