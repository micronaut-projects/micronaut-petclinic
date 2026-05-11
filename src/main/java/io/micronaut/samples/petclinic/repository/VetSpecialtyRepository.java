package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.repository.GenericRepository;
import io.micronaut.samples.petclinic.model.Specialty;
import io.micronaut.samples.petclinic.model.VetSpecialty;
import java.util.List;

/**
 * Repository for the vet-specialty join table.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface VetSpecialtyRepository extends GenericRepository<VetSpecialty, VetSpecialty> {

    /**
     * Saves a join row between a vet and a specialty.
     *
     * @param entity the join row to save
     */
    void save(VetSpecialty entity);

    /**
     * Finds specialties for a vet.
     *
     * @param vetId the vet id
     * @return the specialties associated with the vet
     */
    List<Specialty> findSpecialtiesByVetId(Integer vetId);
}
