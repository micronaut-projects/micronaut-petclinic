package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.model.geo.Point;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/**
 * Request body for clinic searches that submit an ordered coordinate list.
 *
 * @param coordinates coordinates used to build a polygon or line
 * @param acceptingNewPatients optional filter for clinics accepting new patients
 * @param emergencyService optional filter for clinics providing emergency service
 */
@Introspected
@Serdeable
public record ClinicCoordinatesRequest(
        List<ClinicCoordinateDto> coordinates,
        Boolean acceptingNewPatients,
        Boolean emergencyService
) {

    /**
     * Creates an empty request for framework binding.
     */
    public ClinicCoordinatesRequest() {
        this(null, null, null);
    }

    /**
     * Converts submitted latitude/longitude coordinates to Micronaut Data points.
     *
     * @return coordinate list as points where x is longitude and y is latitude
     */
    public List<Point> coordinatesAsPointList() {
        return coordinates.stream().map(ClinicCoordinateDto::asPoint).toList();
    }
}
