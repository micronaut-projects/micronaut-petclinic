package io.micronaut.samples.petclinic.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.samples.petclinic.dto.SpecialityDto;
import io.micronaut.samples.petclinic.dto.VetDto;
import io.micronaut.samples.petclinic.dto.VetForm;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.exception.PreconditionFailedException;
import io.micronaut.samples.petclinic.exception.PreconditionRequiredException;
import io.micronaut.samples.petclinic.mapper.FormMapper;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.service.VetService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.micronaut.http.HttpStatus.CREATED;
import static io.micronaut.http.HttpStatus.NO_CONTENT;

/** Veterinarian and speciality pages and operations. */
@Controller("/vets")
@ExecuteOn(TaskExecutors.BLOCKING)
@Secured(SecurityRule.IS_ANONYMOUS)
public class VetController {

    private final VetService vetService;
    private final FormMapper formMapper;

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
    @Get
    @View("vets/vetList")
    public Map<String, Object> showVetList(@QueryValue(defaultValue = "false") boolean saved) {
        return vetListModel(saved);
    }

    /**
     * Displays the veterinarian list through its HTML alias.
     *
     * @param saved whether to display the saved notice
     * @return the veterinarian list view model
     */
    @Get("/html")
    @View("vets/vetList")
    public Map<String, Object> showResourcesVetList(@QueryValue(defaultValue = "false") boolean saved) {
        return vetListModel(saved);
    }

    /**
     * Lists veterinarians with their fetched specialities as DTOs in JSON.
     *
     * @return the current veterinarian DTOs
     */
    @Get("/json")
    public Collection<VetDto> showResourcesVetListJson() {
        return vetService.findAll().stream().map(formMapper::toVetDto).toList();
    }

    /**
     * Displays an empty veterinarian creation form.
     *
     * @return the form and available specialities
     */
    @Get("/new")
    @View("vets/createOrUpdateVetForm")
    public Map<String, Object> newVetForm() {
        return vetFormModel(new VetDto(), true, false);
    }

    /**
     * Loads the current veterinarian and ETag into the edit page.
     *
     * @param id veterinarian identifier
     * @param saved whether to display the saved notice
     * @return the edit form and available specialities
     * @throws NotFoundException if the vet isn't found
     */
    @Get("/{id}/edit")
    @View("vets/createOrUpdateVetForm")
    public Map<String, Object> editVetForm(@PathVariable Integer id,
                                           @QueryValue(defaultValue = "false") boolean saved) {
        Vet vet = null;
        try {
            vet = vetService.findById(id);
        } catch (NotFoundEntityException e) {
            throw new NotFoundException();
        }
        VetDto form = new VetDto(vet.id(), vet.firstName(), vet.lastName(),
                vet.specialities().stream().map(formMapper::toSpecialityDto).collect(Collectors.toSet()),
                Objects.requireNonNull(vet.etag()));
        return vetFormModel(form, false, saved);
    }

    /**
     * Displays available specialities ordered by name.
     *
     * @param created whether to display the creation confirmation
     * @param changed whether to display the update confirmation
     * @return the speciality list view model
     */
    @Get("/specialities")
    @View("vets/specialityList")
    public Map<String, Object> showSpecialities(@QueryValue(defaultValue = "false") boolean created,
                                                @QueryValue(defaultValue = "false") boolean changed) {
        return Map.of("specialities", vetService.findSpecialities(), "created", created, "changed", changed);
    }

    /**
     * Displays an empty speciality creation form.
     *
     * @param duplicate whether to display the duplicate-name notice
     * @return the creation form view model
     */
    @Get("/specialities/new")
    @View("vets/createOrUpdateSpecialityForm")
    public Map<String, Object> newSpecialityForm(@QueryValue(defaultValue = "false") boolean duplicate) {
        return specialityFormModel(new SpecialityDto(null, null), true, false, duplicate);
    }

    /**
     * Displays the current speciality name for editing.
     *
     * @param id speciality identifier
     * @param saved whether to display the saved notice
     * @param duplicate whether to display the duplicate-name notice
     * @return the speciality edit view model
     * @throws NotFoundException if the speciality isn't found
     */
    @Get("/specialities/{id}/edit")
    @View("vets/createOrUpdateSpecialityForm")
    public Map<String, Object> editSpecialityForm(@PathVariable Integer id,
                                                  @QueryValue(defaultValue = "false") boolean saved,
                                                  @QueryValue(defaultValue = "false") boolean duplicate) {
        Speciality speciality = null;
        try {
            speciality = vetService.findSpecialityById(id);
        } catch (NotFoundEntityException e) {
            throw new NotFoundException();
        }
        return specialityFormModel(formMapper.toSpecialityDto(speciality), false, saved, duplicate);
    }

    /**
     * Creates a veterinarian, resolving assignments against stored specialities.
     *
     * @param form submitted names and speciality IDs
     * @return HTTP 201 with the saved veterinarian and ETag
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     * @throws NotFoundEntityException if the vet isn't found
     */
    @Post(consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<VetDto> createVet(@Valid @Body VetForm form) throws IllegalRequestArgumentException, NotFoundEntityException {
        Vet saved = vetService.create(form);
        return response(saved, HttpStatus.CREATED);
    }

    /**
     * Reads a veterinarian with its ETag and specialities.
     *
     * @param id veterinarian identifier
     * @return HTTP 200 with the veterinarian and quoted ETag header
     */
    @Get("/{id}")
    public HttpResponse<VetDto> readVet(@PathVariable Integer id) throws NotFoundEntityException {
        return response(vetService.findById(id), HttpStatus.OK);
    }

    /**
     * Updates a veterinarian using the ETag supplied in {@code If-Match}.
     *
     * @param id veterinarian identifier from the URL
     * @param ifMatch the current ETag, optionally enclosed in double quotes
     * @param request new names and speciality references
     * @return HTTP 200 with the updated veterinarian and ETag
     * @throws PreconditionRequiredException if the ETag is absent or blank
     * @throws PreconditionFailedException if the veterinarian has changed
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     * @throws NotFoundEntityException if the vet isn't found
     */
    @Put(value = "/{id}", consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<VetDto> updateVet(@PathVariable Integer id,
                                          @Nullable @Header("If-Match") String ifMatch,
                                          @Valid @Body VetForm request)
            throws PreconditionRequiredException, PreconditionFailedException, IllegalRequestArgumentException, NotFoundEntityException {
        Vet current = vetService.findById(id);
        return response(vetService.update(current, request, ifMatch), HttpStatus.OK);
    }

    /**
     * Deletes a veterinarian using the ETag supplied in {@code If-Match}.
     *
     * @param id veterinarian identifier
     * @param ifMatch the current ETag, optionally enclosed in double quotes
     * @throws PreconditionRequiredException if the ETag is absent or blank
     * @throws PreconditionFailedException if the veterinarian has changed
     * @throws NotFoundEntityException if the vet isn't found
     */
    @Delete("/{id}")
    @Status(NO_CONTENT)
    public void deleteVet(@PathVariable Integer id,
                          @Nullable @Header("If-Match") String ifMatch)
            throws PreconditionRequiredException, PreconditionFailedException, NotFoundEntityException {
        vetService.delete(id, ifMatch);
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
