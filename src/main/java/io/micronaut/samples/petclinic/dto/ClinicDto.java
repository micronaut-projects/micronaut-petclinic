package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/**
 * Controller-facing clinic projection used by both HTML and JSON responses.
 *
 * @param id the clinic identifier
 * @param name the clinic name
 * @param address the clinic street address
 * @param city the clinic city
 * @param acceptingNewPatients whether the clinic is accepting new patients
 * @param emergencyService whether the clinic provides emergency service
 * @param latitude the clinic latitude
 * @param longitude the clinic longitude
 */
@Introspected
@Serdeable
public record ClinicDto(
        Integer id,
        String name,
        String address,
        String city,
        Boolean acceptingNewPatients,
        Boolean emergencyService,
        double latitude,
        double longitude
) {

    /**
     * Maps a persisted clinic entity to a DTO.
     *
     * @param clinic the persisted clinic
     * @return the DTO projection
     */
    public static ClinicDto from(Clinic clinic) {
        return new ClinicDto(
                clinic.id(),
                clinic.name(),
                clinic.address(),
                clinic.city(),
                clinic.acceptingNewPatients(),
                clinic.emergencyService(),
                clinic.location().y(),
                clinic.location().x()
        );
    }

    /**
     * Maps a Collection of persisted clinics entities to a Collection of DTOs.
     *
     * @param clinics the persisted clinics
     * @return the DTO projection
     */
    public static List<ClinicDto> from(List<Clinic> clinics) {
        return clinics.stream().map(ClinicDto::from).toList();
    }
}
