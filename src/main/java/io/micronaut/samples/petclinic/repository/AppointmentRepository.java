package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Appointment;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Appointment} entities.
 * <p>
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface AppointmentRepository extends CrudRepository<Appointment, Integer> {
    /**
     * Locks an appointment within the caller's transaction until commit or rollback.
     *
     * @param id appointment id
     * @return the locked appointment, if it exists
     */
    @Override
    @NonNull
    Optional<Appointment> findById(Integer id);

    /** Returns available appointments ordered by display order, then id. */
    List<Appointment> findAvailableAppointments();
}
