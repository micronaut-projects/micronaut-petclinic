package io.micronaut.samples.petclinic.repository;

import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the clinic Boolean columns and derived predicates.
 */
@MicronautTest
class ClinicRepositoryTest {

    @Inject
    ClinicRepository clinicRepository;

    @Test
    void shouldFindClinicsAcceptingNewPatients() {
        List<Clinic> clinics = clinicRepository.findByAcceptingNewPatients(true);

        assertThat(clinics).isNotEmpty()
                .allMatch(Clinic::acceptingNewPatients);
        assertThat(clinics).extracting(Clinic::name)
                .contains("Downtown Madison Pet Clinic", "Capitol Square Pet Clinic")
                .doesNotContain("University Pet Clinic");
    }

    @Test
    void shouldFindClinicsWithoutEmergencyService() {
        List<Clinic> clinics = clinicRepository.findByEmergencyService(false);

        assertThat(clinics).isNotEmpty()
                .allMatch(clinic -> !clinic.emergencyService());
        assertThat(clinics).extracting(Clinic::name)
                .contains("Capitol Square Pet Clinic", "University Pet Clinic")
                .doesNotContain("Downtown Madison Pet Clinic");
    }

    @Test
    void shouldFindClinicsMatchingBothBooleanFlags() {
        List<Clinic> clinics = clinicRepository.findByAcceptingNewPatientsAndEmergencyService(true, true);

        assertThat(clinics).isNotEmpty()
                .allMatch(clinic -> clinic.acceptingNewPatients() && clinic.emergencyService());
        assertThat(clinics).extracting(Clinic::name)
                .contains("Downtown Madison Pet Clinic", "East Madison Pet Clinic")
                .doesNotContain("Capitol Square Pet Clinic", "University Pet Clinic");
    }
}
