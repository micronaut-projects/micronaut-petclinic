package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Index;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Srid;
import io.micronaut.data.model.geo.Point;
import io.micronaut.data.model.geo.LineString;
import io.micronaut.data.model.geo.Polygon;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Entity representing a physical pet clinic branch.
 * <p>
 * The {@code location} property uses WGS 84 coordinates. In Micronaut Data's
 * {@link Point} model, {@code x} is longitude and {@code y} is latitude.
 * {@code serviceArea} is a small polygon representing the clinic's coverage area.
 *
 * @param id the database identifier, or {@code null} for a new clinic
 * @param name the display name of the clinic branch
 * @param address the street address
 * @param city the city
 * @param acceptingNewPatients whether the clinic is currently accepting new patients
 * @param emergencyService whether the clinic provides emergency service
 * @param location the geospatial point for the branch
 * @param serviceArea the geospatial service coverage area for the branch
 */
@MappedEntity("CLINICS")
@Serdeable
public record Clinic(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("NAME")
        @NotBlank
        String name,

        @MappedProperty("ADDRESS")
        @NotBlank
        String address,

        @MappedProperty("CITY")
        @NotBlank
        String city,

        @MappedProperty("ACCEPTING_NEW_PATIENTS")
        @NotNull
        Boolean acceptingNewPatients,

        @MappedProperty("EMERGENCY_SERVICE")
        @NotNull
        Boolean emergencyService,

        @Srid(value = 4326, type = Srid.CrsType.GEOGRAPHIC)
        @Index(columns = "LOCATION")
        @MappedProperty("LOCATION")
        @NotNull
        Point location,

        @Srid(value = 4326, type = Srid.CrsType.GEOGRAPHIC)
        @Index(columns = "SERVICE_AREA")
        @MappedProperty("SERVICE_AREA")
        @NotNull
        Polygon serviceArea
) {

    /**
     * Creates an empty clinic for framework binding.
     */
    public Clinic() {
        this(null, null, null, null, null, null, null, null);
    }

    /**
     * Creates a new clinic without an id.
     *
     * @param name the display name
     * @param address the street address
     * @param city the city
     * @param longitude the longitude coordinate
     * @param latitude the latitude coordinate
     */
    public Clinic(String name, String address, String city, double longitude, double latitude) {
        this(name, address, city, longitude, latitude, true, false);
    }

    /**
     * Creates a new clinic without an id and with its availability flags.
     *
     * @param name the display name
     * @param address the street address
     * @param city the city
     * @param longitude the longitude coordinate
     * @param latitude the latitude coordinate
     * @param acceptingNewPatients whether the clinic is accepting new patients
     * @param emergencyService whether the clinic provides emergency service
     */
    public Clinic(String name,
                  String address,
                  String city,
                  double longitude,
                  double latitude,
                  boolean acceptingNewPatients,
                  boolean emergencyService) {
        this(null,
                name,
                address,
                city,
                acceptingNewPatients,
                emergencyService,
                new Point(longitude, latitude),
                serviceArea(longitude, latitude));
    }

    private static Polygon serviceArea(double longitude, double latitude) {
        double longitudeOffset = 0.0100;
        double latitudeOffset = 0.0080;
        return new Polygon(List.of(new LineString(List.of(
                new Point(longitude - longitudeOffset, latitude - latitudeOffset),
                new Point(longitude - longitudeOffset, latitude + latitudeOffset),
                new Point(longitude + longitudeOffset, latitude + latitudeOffset),
                new Point(longitude + longitudeOffset, latitude - latitudeOffset),
                new Point(longitude - longitudeOffset, latitude - latitudeOffset)
        ))));
    }
}
