package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Upsert;
import io.micronaut.data.repository.GenericRepository;
import io.micronaut.samples.petclinic.model.ClinicServiceOffering;

import java.util.List;
import java.util.Optional;

import static io.micronaut.data.annotation.Join.Type.LEFT_FETCH;

/**
 * Repository for the service catalog belonging to each clinic.
 *
 * <p>The concrete dialect repositories annotate this interface with
 * {@code @JdbcRepository}. The explicit {@link Upsert} method is intentionally
 * opt-in so the example makes the generated native upsert visible.</p>
 */
public interface ClinicServiceOfferingRepository extends GenericRepository<ClinicServiceOffering, Integer> {

    /**
     * Inserts an offering or updates the offering with the same clinic and code.
     *
     * @param offering the clinic-owned offering
     * @return the persisted offering when the driver returns it
     */
    @Upsert(conflictsOn = {"clinicServiceKey"})
    ClinicServiceOffering upsert(ClinicServiceOffering offering);

    /**
     * Deletes an offering by its database identifier.
     *
     * @param id the offering identifier
     */
    void deleteById(Integer id);

    /**
     * Lists a clinic's offerings in stable catalog-code order.
     *
     * @param clinicId the clinic branch
     * @return offerings owned by that clinic
     */
    @Join(value = "clinic", type = LEFT_FETCH)
    List<ClinicServiceOffering> findByClinicIdOrderByServiceCode(Integer clinicId);

    /**
     * Finds one offering within one clinic.
     *
     * @param clinicId the clinic branch
     * @param serviceCode the clinic-local code
     * @return the offering if configured
     */
    @Join(value = "clinic", type = LEFT_FETCH)
    Optional<ClinicServiceOffering> findByClinicIdAndServiceCode(Integer clinicId, String serviceCode);
}
