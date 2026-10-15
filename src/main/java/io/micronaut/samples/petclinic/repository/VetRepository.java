package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.OrderBy;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Vet;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Vet} entities.
 * Uses Micronaut Data JDBC for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface VetRepository extends CrudRepository<Vet, Integer> {

    /**
     * Fetches veterinarians and their specialities ordered by last name.
     *
     * @return veterinarians with their generated ETags
     */
    @NonNull
    @Join(value = "specialities", type = Join.Type.LEFT_FETCH)
    @OrderBy("lastName")
    List<Vet> findAll();

    /**
     * Fetches one veterinarian, its ETag, and its specialities.
     *
     * @param id veterinarian identifier
     * @return the fetched veterinarian, or empty if absent
     */
    @NonNull
    @Override
    @Join(value = "specialities", type = Join.Type.LEFT_FETCH)
    Optional<Vet> findById(Integer id);

    /**
     * Removes old assignments before the update cascade inserts the selected links.
     * Call within the same transaction as the ETag-checked update so a conflict restores them.
     *
     * @param id veterinarian whose assignments should be replaced
     */
    @Query(value = "DELETE FROM VET_SPECIALTIES WHERE VET_ID = :id", nativeQuery = true)
    void deleteSpecialityLinks(Integer id);
}
