package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.generated.OwnerCareSummary;
import io.micronaut.samples.petclinic.mapper.FormMapper;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

/** Downloads owner contact details, pets, and visits as JSON attachments. */
@Controller("/owners/{ownerId}/care-summary")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_ANONYMOUS)
public class OwnerCareSummaryController {

    private final ClinicService clinicService;
    private final FormMapper formMapper;

    /**
     * Creates the controller with its service and mapper dependencies.
     *
     * @param clinicService the service used to load owners and their care history
     * @param formMapper the mapper used to create generated care summary records
     */
    public OwnerCareSummaryController(ClinicService clinicService, FormMapper formMapper) {
        this.clinicService = clinicService;
        this.formMapper = formMapper;
    }

    /**
     * Downloads an owner's care summary using the generated export record.
     *
     * @param ownerId the persisted owner's identifier
     * @return the JSON attachment containing owner details, pets, and visits
     * @throws NotFoundEntityException if the owner does not exist
     */
    @Get(produces = MediaType.APPLICATION_JSON)
    public HttpResponse<OwnerCareSummary> download(@PathVariable Integer ownerId) throws NotFoundEntityException {
        Owner owner = clinicService.findOwnerById(ownerId);
        return HttpResponse.ok(formMapper.toOwnerCareSummary(owner))
                .contentType(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"owner-" + ownerId + "-care-summary.json\"");
    }
}
