package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.dto.ClinicServiceOfferingForm;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.samples.petclinic.model.ClinicServiceOffering;
import io.micronaut.samples.petclinic.repository.ClinicServiceOfferingRepository;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

/**
 * Coordinates the clinic service catalog and its clinic-owned upserts.
 */
@Singleton
public class ClinicServiceOfferingCatalogService {

    private final ClinicServiceOfferingRepository offeringRepository;
    private final ClinicService clinicService;

    /**
     * Creates the catalog service.
     *
     * @param offeringRepository repository for clinic-owned offerings
     */
    public ClinicServiceOfferingCatalogService(ClinicServiceOfferingRepository offeringRepository, ClinicService clinicService) {
        this.offeringRepository = offeringRepository;
        this.clinicService = clinicService;
    }

    /**
     * Lists all offerings owned by a clinic.
     *
     * @param clinicId the clinic branch
     * @return the clinic's offerings
     */
    public List<ClinicServiceOffering> listForClinic(Integer clinicId) {
        return offeringRepository.findByClinicIdOrderByServiceCode(clinicId);
    }

    /**
     * Finds one offering owned by a clinic.
     *
     * @param clinicId the clinic branch
     * @param serviceCode the clinic-local service code
     * @return the offering if configured
     */
    public Optional<ClinicServiceOffering> findForClinic(Integer clinicId, String serviceCode) {
        return offeringRepository.findByClinicIdAndServiceCode(clinicId, serviceCode);
    }

    /**
     * Persists a clinic offering through the native upsert operation.
     *
     * @param clinicId the clinic branch from the URL
     * @param form validated offering fields
     * @return the entity passed to the repository
     */
    public ClinicServiceOffering upsert(Integer clinicId, ClinicServiceOfferingForm form) {
        Clinic clinic = clinicService.findClinicById(clinicId);
        ClinicServiceOffering offering = new ClinicServiceOffering(
                clinic, form.serviceCode(),
                form.name(),
                form.description(),
                form.price(),
                form.durationMinutes());
        return offeringRepository.upsert(offering);
    }

}
