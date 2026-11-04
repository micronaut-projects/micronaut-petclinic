package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.samples.petclinic.dto.SpecialityDto;
import io.micronaut.samples.petclinic.dto.VetDto;
import io.micronaut.samples.petclinic.dto.VetForm;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.mapper.FormMapper;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.samples.petclinic.service.VetService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static io.micronaut.http.HttpStatus.CREATED;

/** Veterinarian and speciality pages and operations. */
@Controller("/vets")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_ANONYMOUS)
public class VetController {

    private final VetService vetService;
    private final FormMapper formMapper;
    private static final String VIEW_VET_FORM = "vets/createOrUpdateVetForm";

    private final ClinicService clinicService;

    /**
     * Creates the veterinarian and speciality controller.
     *
     * @param vetService service for persistence and ETag checks
     * @param formMapper maps request and response DTOs
     */
    public VetController(VetService vetService,
                         FormMapper formMapper) {
        this.vetService = vetService;
        this.formMapper = formMapper;
    }

    /**
     * Displays veterinarians ordered by last name.
     *
     * @param saved whether to display the saved notice
     * @return the veterinarian list view model
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get
    @View("vets/vetList")
    public Map<String, Object> showVetList(@QueryValue(defaultValue = "false") boolean saved) {
        return vetListModel(saved);
    }

    /**
     * Display the list of veterinarians in HTML format.
     * Alias for the main endpoint.
     * @return the vets list view
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/html")
    @View("vets/vetList")
    public Map<String, Object> showResourcesVetList() {
        return showVetList();
    }

    /**
     * Return the list of veterinarians as JSON.
     * @return collection of vets in JSON format
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Vet> showResourcesVetListJson() {
        return clinicService.findAllVets();
    }

    /**
     * Display the form to create a new veterinarian.
     *
     * @return the create vet form view
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/new")
    @View(VIEW_VET_FORM)
    public Map<String, Object> initCreationForm() {
        return Map.of(
                "vet", new VetForm(),
                "validationErrors", Map.of()
        );
    }

    /**
     * Process the form to create a new veterinarian.
     *
     * @param form the submitted vet form data
     * @return redirect to the vet list on success
     */
    @Secured({"ROLE_ADMIN"})
    @Post(value = "/new", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> processCreationForm(@Valid @Body VetForm form) {
        clinicService.saveVet(new Vet(form.firstName(), form.lastName()));
        URI uri = UriBuilder.of("/vets").build();
        return HttpResponse.redirect(uri);
    }

    /**
     * Renders the vet creation form when validation fails.
     *
     * @param request the request containing the submitted form
     * @param e the validation exception
     * @return a form model containing submitted values and field errors
     */
    @Error(exception = ConstraintViolationException.class)
    @View(VIEW_VET_FORM)
    public Map<String, Object> onCreateVetValidationError(HttpRequest<?> request,
                                                          ConstraintViolationException e) {
        VetForm vet = request.getBody(VetForm.class).orElseGet(VetForm::new);

        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            String field = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
            int lastDot = field.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < field.length() - 1) {
                field = field.substring(lastDot + 1);
            }
            errors.put(field, v.getMessage());
        }
        return Map.of(
                "vet", vet,
                "validationErrors", errors
        );
    }

    /**
     * Lists available specialities as JSON.
     *
     * @return speciality identifiers and names
     */
    @Get("/specialities/json")
    public Collection<SpecialityDto> listSpecialities() {
        return vetService.findSpecialities().stream().map(formMapper::toSpecialityDto).toList();
    }

    /**
     * Creates a speciality through the JSON API.
     *
     * @param request submitted speciality name
     * @return HTTP 201 with the persisted speciality
     * @throws IllegalRequestArgumentException if the name already exists
     */
    @Post(value = "/specialities", consumes = MediaType.APPLICATION_JSON)
    @Status(CREATED)
    public SpecialityDto createSpeciality(@Valid @Body SpecialityDto request) throws IllegalRequestArgumentException {
        Speciality saved = vetService.createSpeciality(request);
        return formMapper.toSpecialityDto(saved);
    }

    /**
     * Renames a speciality while preserving its assignments.
     *
     * @param id speciality identifier from the URL
     * @param request submitted speciality name
     * @return HTTP 200 with the updated speciality
     * @throws IllegalRequestArgumentException if another speciality has that name
     * @throws NotFoundEntityException if the speciality isn't found
     */
    @Put(value = "/specialities/{id}", consumes = MediaType.APPLICATION_JSON)
    public SpecialityDto updateSpeciality(@PathVariable Integer id,
                                          @Valid @Body SpecialityDto request) throws IllegalRequestArgumentException, NotFoundEntityException {
        if (request.id() != null && !Objects.equals(id, request.id())) {
            throw new IllegalRequestArgumentException("Id should match the one in the url");
        }
        Speciality updated = vetService.updateSpeciality(id, request);
        return formMapper.toSpecialityDto(updated);
    }

    private HttpResponse<VetDto> response(Vet vet, HttpStatus status) {
        VetDto dto = formMapper.toVetDto(vet);
        return HttpResponse.status(status)
                .header("ETag", "\"" + dto.etag() + "\"")
                .header("Cache-Control", "no-store")
                .body(dto);
    }

    private Map<String, Object> vetFormModel(VetDto form, boolean isNew, boolean saved) {
        return Map.of("vetForm", form,
                "specialities", vetService.findSpecialities(),
                "isNew", isNew,
                "saved", saved);
    }

    private Map<String, Object> vetListModel(boolean saved) {
        return Map.of("vets", vetService.findAll(), "saved", saved);
    }

    private static Map<String, Object> specialityFormModel(SpecialityDto form,
                                                           boolean isNew,
                                                           boolean saved,
                                                           boolean duplicate) {
        return Map.of("specialityForm", form, "isNew", isNew, "saved", saved, "duplicate", duplicate);
    }

    /**
     * Reports invalid request data.
     *
     * @param error the request failure
     * @return HTTP 400 with a JSON message
     */
    @Error(exception = ConstraintViolationException.class)
    public HttpResponse<Map<String, String>> validationError(ConstraintViolationException error) {
        return HttpResponse.badRequest(Map.of("message", "Invalid request."));
    }
}
