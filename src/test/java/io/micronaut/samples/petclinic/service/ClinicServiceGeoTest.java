package io.micronaut.samples.petclinic.service;

import io.micronaut.data.model.geo.Point;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Geospatial integration tests for {@link ClinicService}.
 */
@MicronautTest
class ClinicServiceGeoTest {

    @Inject
    ClinicService clinicService;

    @Test
    void shouldFindClinicsNearPoint() {
        Collection<Clinic> clinics = clinicService.findClinicsNear(-89.3840, 43.0745, 350);
        assertThat(clinics).isNotEmpty();
        assertThat(clinics).extracting(Clinic::name)
                .contains("Downtown Madison Pet Clinic", "Capitol Square Pet Clinic")
                .doesNotContain("University Pet Clinic", "Milwaukee Pet Clinic");
    }

    @Test
    void shouldExpandClinicsAsNearbyRadiusGrows() {
        Collection<Clinic> smallRadiusClinics = clinicService.findClinicsNear(-89.3840, 43.0745, 350);
        Collection<Clinic> largerRadiusClinics = clinicService.findClinicsNear(-89.3840, 43.0745, 5000);

        assertThat(smallRadiusClinics).isNotEmpty();
        assertThat(largerRadiusClinics.size()).isGreaterThan(smallRadiusClinics.size());
        assertThat(smallRadiusClinics).extracting(Clinic::name)
                .contains("Downtown Madison Pet Clinic", "Capitol Square Pet Clinic");
        assertThat(largerRadiusClinics).extracting(Clinic::name)
                .contains("University Pet Clinic", "East Madison Pet Clinic");
    }

    @Test
    void shouldFilterNearbyClinicsByAvailability() {
        Collection<Clinic> clinics = clinicService.findClinicsNear(
                -89.3840, 43.0745, 5000, true, true);

        assertThat(clinics).isNotEmpty();
        assertThat(clinics).allMatch(clinic -> clinic.acceptingNewPatients() && clinic.emergencyService());
        assertThat(clinics).extracting(Clinic::name)
                .contains("Downtown Madison Pet Clinic", "East Madison Pet Clinic")
                .doesNotContain("Capitol Square Pet Clinic", "University Pet Clinic");
    }

    @Test
    void shouldFilterPolygonClinicsByNewPatientAvailability() {
        Collection<Clinic> clinics = clinicService.findClinicsWithinBounds(
                -89.55, 43.00, -89.20, 43.20, false, null);

        assertThat(clinics).isNotEmpty();
        assertThat(clinics).allMatch(clinic -> !clinic.acceptingNewPatients());
        assertThat(clinics).extracting(Clinic::name)
                .contains("University Pet Clinic", "South Madison Pet Clinic")
                .doesNotContain("Downtown Madison Pet Clinic");
    }

    @Test
    void shouldFindClinicsWithinBounds() {
        Collection<Clinic> clinics = clinicService.findClinicsWithinBounds(-89.55, 43.00, -89.20, 43.20);
        assertThat(clinics).isNotEmpty();
        assertThat(clinics).extracting(Clinic::name)
                .contains("Downtown Madison Pet Clinic", "Monona Pet Clinic")
                .doesNotContain("Milwaukee Pet Clinic");
    }

    @Test
    void shouldRejectSelfIntersectingWithinPolygon() {
        List<Point> coordinates = List.of(
                new Point(-89.90936279296876, 42.9023053660702),
                new Point(-89.38751220703125, 42.92845469924115),
                new Point(-88.94119262695312, 43.0961493137274),
                new Point(-89.33395385742189, 43.25038397145106),
                new Point(-89.42733764648438, 43.21736647998013),
                new Point(-89.47402954101564, 42.962633237852216),
                new Point(-89.09637451171876, 43.069981133454796),
                new Point(-89.90798950195314, 43.015873699839446),
                new Point(-89.87091064453125, 43.14727016282878),
                new Point(-89.176025390625, 43.135155533852306),
                new Point(-89.65667724609376, 42.967657892987084),
                new Point(-89.10598754882812, 42.97670123799119),
                new Point(-89.50286865234376, 43.14025610479275),
                new Point(-89.90936279296876, 42.9023053660702)
        );

        assertThatThrownBy(() -> clinicService.findClinicsWithinPolygon(coordinates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A polygon boundary cannot cross itself");
    }

    @Test
    void shouldFindClinicsIntersectingLine() {
        Collection<Clinic> clinics = clinicService.findClinicsIntersectingLine(List.of(
                new Point(-89.5500, 43.0650),
                new Point(-89.3300, 43.1100),
                new Point(-89.2000, 43.1900)
        ));
        assertThat(clinics).isNotEmpty();
        assertThat(clinics).extracting(Clinic::name)
                .contains("West Madison Pet Clinic", "East Madison Pet Clinic", "Sun Prairie Pet Clinic")
                .doesNotContain("Downtown Madison Pet Clinic", "Janesville Pet Clinic");
    }
}
