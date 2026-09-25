package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.samples.petclinic.dto.ClinicServiceOfferingForm;
import io.micronaut.samples.petclinic.dto.ClinicServiceOfferingResponse;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.samples.petclinic.model.ClinicServiceOffering;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.samples.petclinic.service.ClinicServiceOfferingCatalogService;
import io.micronaut.views.ModelAndView;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Web UI for maintaining the offerings owned by one clinic branch.
 */
@Controller("/clinics/{clinicId}/services")
class ClinicServiceOfferingController {

    private final ClinicServiceOfferingCatalogService catalogService;
    private final ClinicService clinicService;

    /**
     * Creates the catalog controller.
     *
     * @param catalogService service for clinic-owned offerings
     */
    ClinicServiceOfferingController(ClinicServiceOfferingCatalogService catalogService,
                                    ClinicService clinicService) {
        this.catalogService = catalogService;
        this.clinicService = clinicService;
    }

    /**
     * Displays all offerings for one clinic.
     *
     * @param clinicId the clinic branch
     * @return the clinic catalog page
     */
    @Get
    @View("clinics/servicesList")
    public Map<String, Object> list(@PathVariable Integer clinicId) {
        Clinic clinic = clinicService.findClinicById(clinicId);
        return Map.of(
                "clinic", clinic,
                "services", catalogService.listForClinic(clinicId));
    }

    /**
     * Displays the form for a new offering.
     *
     * @param clinicId the clinic branch
     * @return the create form
     */
    @Get("/new")
    @View("clinics/createOrUpdateServiceForm")
    public Map<String, Object> newForm(@PathVariable Integer clinicId) {
        return formModel(clinicId, new ClinicServiceOfferingForm(), true, Map.of());
    }

    /**
     * Displays the form for an existing clinic offering.
     *
     * @param clinicId the clinic branch
     * @param serviceCode the clinic-local service code
     * @return the update form
     */
    @Get("/{serviceCode}/edit")
    @View("clinics/createOrUpdateServiceForm")
    public Map<String, Object> editForm(@PathVariable Integer clinicId,
                                        @PathVariable String serviceCode) {
        ClinicServiceOffering offering = catalogService.findForClinic(clinicId, serviceCode)
                .orElseThrow(NotFoundException::new);
        return formModel(clinicId, ClinicServiceOfferingForm.from(offering), false, Map.of());
    }

    /**
     * Creates an offering or updates the same clinic/code combination.
     *
     * @param clinicId the clinic branch
     * @param form validated form values
     * @return the persisted offering as JSON
     */
    @Post(consumes = MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<ClinicServiceOfferingResponse> create(@PathVariable Integer clinicId,
                                                              @Valid @Body ClinicServiceOfferingForm form) {
        return HttpResponse.ok(ClinicServiceOfferingResponse.from(catalogService.upsert(clinicId, form)));
    }

    /**
     * Updates an existing catalog entry using the same upsert operation.
     *
     * @param clinicId the clinic branch
     * @param serviceCode path code used to guard against changing the key
     * @param form validated form values
     * @return the persisted offering as JSON
     */
    @Post(value = "/{serviceCode}", consumes = MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<?> update(@PathVariable Integer clinicId,
                                  @PathVariable String serviceCode,
                                  @Valid @Body ClinicServiceOfferingForm form) {
        if (!serviceCode.equals(form.serviceCode())) {
            return HttpResponse.badRequest(Map.of(
                    "message", "The service code cannot be changed while editing an offering"));
        }
        return HttpResponse.ok(ClinicServiceOfferingResponse.from(catalogService.upsert(clinicId, form)));
    }

    /**
     * Renders submitted values when bean validation rejects a form.
     *
     * @param request the submitted form request
     * @param exception validation details
     * @return the form model with inline errors
     */
    @Error(exception = ConstraintViolationException.class)
    public Object onValidationError(HttpRequest<?> request,
                                    ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (var violation : exception.getConstraintViolations()) {
            String field = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
            int lastDot = field.lastIndexOf('.');
            if (lastDot >= 0) {
                field = field.substring(lastDot + 1);
            }
            if (!field.isBlank()) {
                errors.put(field, violation.getMessage());
            }
        }

        boolean jsonRequest = request.getContentType()
                .map(contentType -> MediaType.APPLICATION_JSON.equals(contentType.getName()))
                .orElse(false);
        if (jsonRequest) {
            return HttpResponse.badRequest(Map.of(
                    "message", "Validation failed",
                    "errors", errors));
        }

        Integer clinicId = request.getParameters().get("clinicId", Integer.class).orElse(null);
        if (clinicId == null) {
            throw new NotFoundException();
        }

        ClinicServiceOfferingForm form = request.getBody(ClinicServiceOfferingForm.class)
                .orElseGet(ClinicServiceOfferingForm::new);
        boolean isNew = request.getPath().equals("/clinics/" + clinicId + "/services");
        return new ModelAndView<>("clinics/createOrUpdateServiceForm",
                formModel(clinicId, form, isNew, errors));
    }

    private Map<String, Object> formModel(Integer clinicId,
                                          ClinicServiceOfferingForm form,
                                          boolean isNew,
                                          Map<String, String> errors) {
        return Map.of("clinic", clinicService.findClinicById(clinicId),
                "service", form,
                "isNew", isNew,
                "validationErrors", errors);
    }

}
