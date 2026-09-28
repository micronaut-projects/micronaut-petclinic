package io.micronaut.samples.petclinic.repository;

import io.micronaut.samples.petclinic.model.ClinicServiceOffering;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the clinic-scoped native upsert repository method.
 */
@MicronautTest
class ClinicServiceOfferingRepositoryTest {

    private static final String SERVICE_CODE = "UPSERT_DEMO";

    private final List<Integer> testClinicIds = new ArrayList<>();

    @Inject
    ClinicServiceOfferingRepository offeringRepository;

    @Inject
    ClinicRepository clinicRepository;

    @AfterEach
    void removeTestClinics() {
        for (Integer clinicId : testClinicIds) {
            offeringRepository.findByClinicIdOrderByServiceCode(clinicId)
                    .forEach(offering -> offeringRepository.deleteById(offering.id()));
            clinicRepository.deleteById(clinicId);
        }
        testClinicIds.clear();
    }

    @Test
    void shouldInsertThenUpdateTheSameClinicOffering() {
        Clinic clinic = testClinic("Upsert Clinic", -89.70, 43.08);
        offeringRepository.upsert(new ClinicServiceOffering(
                clinic, SERVICE_CODE, "Wellness check", "Initial description", new BigDecimal("45.00"), 30));

        ClinicServiceOffering inserted = offeringRepository
                .findByClinicIdAndServiceCode(clinic.id(), SERVICE_CODE)
                .orElseThrow();

        offeringRepository.upsert(new ClinicServiceOffering(
                clinic, SERVICE_CODE, "Extended wellness check", "Updated description", new BigDecimal("65.00"), 45));

        ClinicServiceOffering updated = offeringRepository
                .findByClinicIdAndServiceCode(clinic.id(), SERVICE_CODE)
                .orElseThrow();

        assertThat(inserted.id()).isNotNull();
        assertThat(updated.id())
                .as("upsert must update the existing row instead of inserting a new offering")
                .isEqualTo(inserted.id());
        assertThat(updated.name()).isEqualTo("Extended wellness check");
        assertThat(updated.description()).isEqualTo("Updated description");
        assertThat(updated.price()).isEqualByComparingTo("65.00");
        assertThat(updated.durationMinutes()).isEqualTo(45);
        assertThat(offeringRepository.findByClinicIdOrderByServiceCode(clinic.id()))
                .extracting(ClinicServiceOffering::serviceCode)
                .containsExactly(SERVICE_CODE);
    }

    @Test
    void shouldKeepTheSameServiceCodeIndependentPerClinic() {
        Clinic downtownClinic = testClinic("Upsert Downtown Clinic", -89.71, 43.08);
        Clinic capitolClinic = testClinic("Upsert Capitol Clinic", -89.72, 43.08);
        offeringRepository.upsert(new ClinicServiceOffering(
                downtownClinic, SERVICE_CODE, "Downtown wellness check", null, new BigDecimal("45.00"), 30));
        offeringRepository.upsert(new ClinicServiceOffering(
                capitolClinic, SERVICE_CODE, "Capitol wellness check", null, new BigDecimal("55.00"), 35));

        assertThat(offeringRepository.findByClinicIdAndServiceCode(downtownClinic.id(), SERVICE_CODE))
                .get()
                .extracting(ClinicServiceOffering::name)
                .isEqualTo("Downtown wellness check");
        assertThat(offeringRepository.findByClinicIdAndServiceCode(capitolClinic.id(), SERVICE_CODE))
                .get()
                .extracting(ClinicServiceOffering::name)
                .isEqualTo("Capitol wellness check");
    }

    @Test
    void shouldSeedClinicSpecificOfferingCatalogs() {
        Clinic downtownClinic = clinicByName("Downtown Madison Pet Clinic");
        Clinic capitolClinic = clinicByName("Capitol Square Pet Clinic");

        assertThat(offeringRepository.findByClinicIdOrderByServiceCode(downtownClinic.id()))
                .extracting(ClinicServiceOffering::serviceCode)
                .contains("WELLNESS_CHECK", "SENIOR_PET_SCREENING")
                .doesNotContain("RABIES_EXPRESS");
        assertThat(offeringRepository.findByClinicIdOrderByServiceCode(capitolClinic.id()))
                .extracting(ClinicServiceOffering::serviceCode)
                .contains("RABIES_EXPRESS", "APARTMENT_PET_BEHAVIOR")
                .doesNotContain("WELLNESS_CHECK");
    }

    private Clinic testClinic(String name, double longitude, double latitude) {
        Clinic clinic = clinicRepository.save(new Clinic(name, name + " address", "Madison", longitude, latitude));
        testClinicIds.add(clinic.id());
        return clinic;
    }

    private Clinic clinicByName(String name) {
        return clinicRepository.findAll().stream()
                .filter(clinic -> name.equals(clinic.name()))
                .findFirst()
                .orElseThrow();
    }
}
