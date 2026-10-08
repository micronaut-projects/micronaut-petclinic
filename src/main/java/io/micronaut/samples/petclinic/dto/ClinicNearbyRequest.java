package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Request body for nearby clinic searches.
 *
 * @param latitude the latitude coordinate
 * @param longitude the longitude coordinate
 * @param radiusMeters the search radius in meters
 * @param acceptingNewPatients optional filter for clinics accepting new patients
 * @param emergencyService optional filter for clinics providing emergency service
 */
@Introspected
@Serdeable
public record ClinicNearbyRequest(
        double latitude,
        double longitude,
        double radiusMeters,
        Boolean acceptingNewPatients,
        Boolean emergencyService
) {

    /**
     * Creates an empty request for framework binding.
     */
    public ClinicNearbyRequest() {
        this(0, 0, 0, null, null);
    }

    /**
     * Creates a nearby request without availability filters.
     */
    public ClinicNearbyRequest(double latitude, double longitude, double radiusMeters) {
        this(latitude, longitude, radiusMeters, null, null);
    }
}
