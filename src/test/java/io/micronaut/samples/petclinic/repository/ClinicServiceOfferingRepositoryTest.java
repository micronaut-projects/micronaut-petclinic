package io.micronaut.samples.petclinic.repository;

import io.micronaut.samples.petclinic.model.ClinicServiceOffering;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the clinic-scoped native upsert repository method.
 */
@MicronautTest
class ClinicServiceOfferingRepositoryTest {

    private static final String SERVICE_CODE = "UPSERT_DEMO";

    @Inject
    ClinicServiceOfferingRepository offeringRepository;

    @Inject
    ClinicRepository clinicRepository;

    @AfterEach
    void removeDemoRows() {
        offeringRepository.findByClinicIdAndServiceCode(1, SERVICE_CODE)
                .ifPresent(offering -> offeringRepository.deleteById(offering.id()));
        offeringRepository.findByClinicIdAndServiceCode(2, SERVICE_CODE)
                .ifPresent(offering -> offeringRepository.deleteById(offering.id()));
    }

    @Test
    void shouldInsertThenUpdateTheSameClinicOffering() {
        offeringRepository.upsert(new ClinicServiceOffering(
                clinic(1), SERVICE_CODE, "Wellness check", "Initial description", new BigDecimal("45.00"), 30));

        ClinicServiceOffering inserted = offeringRepository
                .findByClinicIdAndServiceCode(1, SERVICE_CODE)
                .orElseThrow();

        offeringRepository.upsert(new ClinicServiceOffering(
                clinic(1), SERVICE_CODE, "Extended wellness check", "Updated description", new BigDecimal("65.00"), 45));

        ClinicServiceOffering updated = offeringRepository
                .findByClinicIdAndServiceCode(1, SERVICE_CODE)
                .orElseThrow();

        assertThat(updated.id()).isEqualTo(inserted.id());
        assertThat(updated.name()).isEqualTo("Extended wellness check");
        assertThat(updated.description()).isEqualTo("Updated description");
        assertThat(updated.price()).isEqualByComparingTo("65.00");
        assertThat(updated.durationMinutes()).isEqualTo(45);
        assertThat(offeringRepository.findByClinicIdOrderByServiceCode(1))
                .extracting(ClinicServiceOffering::serviceCode)
                .contains(SERVICE_CODE);
    }

    @Test
    void shouldKeepTheSameServiceCodeIndependentPerClinic() {
        offeringRepository.upsert(new ClinicServiceOffering(
                clinic(1), SERVICE_CODE, "Downtown wellness check", null, new BigDecimal("45.00"), 30));
        offeringRepository.upsert(new ClinicServiceOffering(
                clinic(2), SERVICE_CODE, "Capitol wellness check", null, new BigDecimal("55.00"), 35));

        assertThat(offeringRepository.findByClinicIdAndServiceCode(1, SERVICE_CODE))
                .get()
                .extracting(ClinicServiceOffering::name)
                .isEqualTo("Downtown wellness check");
        assertThat(offeringRepository.findByClinicIdAndServiceCode(2, SERVICE_CODE))
                .get()
                .extracting(ClinicServiceOffering::name)
                .isEqualTo("Capitol wellness check");
    }

    @Test
    void shouldSeedClinicSpecificOfferingCatalogs() {
        assertThat(offeringRepository.findByClinicIdOrderByServiceCode(1))
                .extracting(ClinicServiceOffering::serviceCode)
                .contains("WELLNESS_CHECK", "SENIOR_PET_SCREENING")
                .doesNotContain("RABIES_EXPRESS");
        assertThat(offeringRepository.findByClinicIdOrderByServiceCode(2))
                .extracting(ClinicServiceOffering::serviceCode)
                .contains("RABIES_EXPRESS", "APARTMENT_PET_BEHAVIOR")
                .doesNotContain("WELLNESS_CHECK");
    }

    private Clinic clinic(Integer id) {
        return clinicRepository.findById(id).orElseThrow();
    }
}
