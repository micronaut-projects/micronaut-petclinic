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

    /** Returns only the two marked showcase appointments, in display order. */
    List<Appointment> findDemoAppointments();

    /**
     * Locks the showcase rows in the caller's transaction, failing immediately
     * on a conflicting row lock. Call before resetting within the same transaction.
     */
    List<Appointment> lockDemoAppointments();

}
