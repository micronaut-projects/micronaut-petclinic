package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Appointment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
     * Lock-wait behavior is dialect-specific; Oracle waits up to ten seconds.
     *
     * @param id appointment id
     * @return the locked appointment, if it exists
     */
    @NonNull
    Optional<Appointment> findByIdForUpdate(Integer id);

    /**
     * Returns available appointments for one showcase, ordered by display order, then id.
     *
     * @param scenario showcase whose appointments are returned
     * @return available appointments in display order
     */
    List<Appointment> findAvailableAppointments(Appointment.Scenario scenario);

    /**
     * Returns available appointments from the priority showcase.
     *
     * @return priority appointments ordered by display order, then id
     */
    default List<Appointment> findAvailableAppointments() {
        return findAvailableAppointments(Appointment.Scenario.PRIORITY);
    }

    /**
     * Returns all appointments belonging to a showcase, ordered by display order, then id.
     *
     * @param scenario showcase whose appointments are returned
     * @return appointments in display order
     */
    List<Appointment> findByScenarioOrderByDisplayOrderAscAndIdAsc(Appointment.Scenario scenario);

    /**
     * Returns appointments in a showcase with the given booking status, ordered by display order, then id.
     *
     * @param scenario showcase whose appointments are searched
     * @param status booking status to match
     * @return matching appointments in display order
     */
    List<Appointment> findByScenarioAndStatusOrderByDisplayOrderAscAndIdAsc(
            Appointment.Scenario scenario, Appointment.Status status);

    /**
     * Finds an appointment only if it belongs to the specified showcase.
     *
     * @param id appointment identifier
     * @param scenario required showcase
     * @return the appointment when both the identifier and showcase match
     */
    Optional<Appointment> findByIdAndScenario(@NotNull @Positive Integer id, Appointment.Scenario scenario);

    /**
     * Locks and finds an appointment only if it belongs to the specified showcase.
     * The lock is held until the caller's transaction commits or rolls back.
     *
     * @param id appointment identifier
     * @param scenario required showcase
     * @return the locked appointment when both the identifier and showcase match
     */
    Optional<Appointment> findByIdAndScenarioForUpdate(@NotNull @Positive Integer id, Appointment.Scenario scenario);

}
