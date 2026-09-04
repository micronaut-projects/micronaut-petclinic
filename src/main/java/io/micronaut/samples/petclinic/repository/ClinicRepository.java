package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.model.geo.Geometry;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Clinic;

import java.util.List;

/**
 * Repository for {@link Clinic} entities.
 * <p>
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface ClinicRepository extends CrudRepository<Clinic, Integer> {

    /**
     * Finds clinics whose point location is within the supplied geometry.
     *
     * @param geometry the search geometry, typically a polygon
     * @return matching clinics
     */
    List<Clinic> findByLocationGeoWithin(Geometry geometry);

    /**
     * Finds clinics whose service area intersects the supplied geometry.
     *
     * @param geometry the search geometry, typically a polygon or line
     * @return matching clinics
     */
    List<Clinic> findByServiceAreaGeoIntersects(Geometry geometry);

    /**
     * Finds clinics within the supplied distance of a point.
     *
     * @param geometry the search origin; x is longitude and y is latitude when a point is supplied
     * @param distance the dialect-specific search distance
     * @return nearby clinics
     */
    List<Clinic> findByLocationNear(Geometry geometry, double distance);

    /**
     * Finds clinics by their new-patient availability.
     *
     * @param acceptingNewPatients the availability value to match
     * @return clinics with the requested availability
     */
    List<Clinic> findByAcceptingNewPatients(Boolean acceptingNewPatients);

    /**
     * Finds clinics by whether they provide emergency service.
     *
     * @param emergencyService the emergency-service value to match
     * @return clinics with the requested service flag
     */
    List<Clinic> findByEmergencyService(Boolean emergencyService);

    /**
     * Finds clinics matching both Boolean availability flags.
     *
     * @param acceptingNewPatients the new-patient availability value
     * @param emergencyService the emergency-service value
     * @return clinics matching both flags
     */
    List<Clinic> findByAcceptingNewPatientsAndEmergencyService(Boolean acceptingNewPatients,
                                                                Boolean emergencyService);
}
