package io.micronaut.samples.petclinic.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Status;
import io.micronaut.samples.petclinic.dto.SpecialityDto;
import io.micronaut.samples.petclinic.dto.VetDto;
import io.micronaut.samples.petclinic.dto.VetForm;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.MissingEtagException;
import io.micronaut.samples.petclinic.exception.StaleEtagException;
import io.micronaut.samples.petclinic.mapper.FormMapper;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.service.VetService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

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
     * @return the veterinarian list view model
     */
    @Get
    @View("vets/vetList")
    public Map<String, Object> showVetList() {
        return Map.of("vets", vetService.findAll());
    }

    /**
     * Displays the veterinarian list through its HTML alias.
     *
     * @return the veterinarian list view model
     */
    @Get("/html")
    @View("vets/vetList")
    public Map<String, Object> showResourcesVetList() {
        return showVetList();
    }

    /**
     * Lists veterinarians with their fetched specialities as JSON.
     *
     * @return the current veterinarians
     */
    @Get("/json")
    public Collection<Vet> showResourcesVetListJson() {
        return vetService.findAll();
    }

    /**
     * Displays an empty veterinarian creation form.
     *
     * @return the form and available specialities
     */
    @Get("/new")
    @View("vets/createOrUpdateVetForm")
    public Map<String, Object> newVetForm() {
        return vetFormModel(new VetForm(), true, false, false);
    }

    /**
     * Creates a veterinarian from the URL-encoded creation form.
     *
     * @param form submitted names and speciality IDs
     * @return a redirect to the veterinarian list
     * @throws IllegalRequestArgumentException if a speciality ID is invalid
     */
    @Post(value = "/new", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    public HttpResponse<?> createVetForm(@Valid @Body VetForm form) throws IllegalRequestArgumentException {
        vetService.create(form);
        return HttpResponse.redirect(URI.create("/vets?saved=true"));
    }

    /**
     * Loads the current veterinarian and ETag into the edit page.
     *
     * @param id veterinarian identifier
     * @param conflict whether to display the conflict notice
     * @param saved whether to display the saved notice
     * @return the edit form and available specialities
     */
    @Get("/{id}/edit")
    @View("vets/createOrUpdateVetForm")
    public Map<String, Object> editVetForm(@PathVariable Integer id,
                                           @QueryValue(defaultValue = "false") boolean conflict,
                                           @QueryValue(defaultValue = "false") boolean saved) {
        Vet vet = vetService.findById(id);
        VetForm form = new VetForm(vet.id(), vet.firstName(), vet.lastName(),
                vet.specialities().stream().map(Speciality::id).collect(java.util.stream.Collectors.toSet()),
                vet.etag());
        return vetFormModel(form, false, conflict, saved);
    }

    /**
     * Saves the JSON edit form using its ETag for optimistic locking.
     *
     * @param id veterinarian identifier from the URL
     * @param form submitted fields and the ETag originally loaded
     * @return HTTP 200 with the saved veterinarian and current ETag
     * @throws IllegalRequestArgumentException if a speciality ID is invalid
     * @throws StaleEtagException if the veterinarian has changed
     */
    @Post(value = "/{id}/edit", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public HttpResponse<VetDto> updateVetForm(@PathVariable Integer id, @Valid @Body VetForm form)
            throws IllegalRequestArgumentException, StaleEtagException {
        Vet updated = vetService.update(vetService.findById(id), form);
        return response(updated, HttpStatus.OK);
    }

    /**
     * Deletes a veterinarian using the ETag supplied in {@code If-Match}.
     *
     * @param id veterinarian identifier
     * @param ifMatch the ETag originally loaded by the edit page
     * @throws MissingEtagException if the ETag is absent or blank
     * @throws StaleEtagException if the veterinarian has changed
     */
    @Delete(value = "/{id}/delete", produces = MediaType.APPLICATION_JSON)
    @Status(NO_CONTENT)
    public void deleteVetForm(@PathVariable Integer id,
                              @Nullable @Header("If-Match") String ifMatch) throws MissingEtagException, StaleEtagException {
        vetService.delete(id, requiredEtag(ifMatch));
    }

    /**
     * Displays available specialities ordered by name.
     *
     * @return the speciality list view model
     */
    @Get("/specialities")
    @View("vets/specialityList")
    public Map<String, Object> showSpecialities() {
        return Map.of("specialities", vetService.findSpecialities());
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
     * Creates a speciality from the JSON form submission.
     *
     * @param form submitted speciality name
     * @return HTTP 201 with the persisted speciality
     * @throws IllegalRequestArgumentException if the name already exists
     */
    @Post(value = "/specialities/new", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public HttpResponse<SpecialityDto> createSpecialityForm(@Valid @Body SpecialityDto form)
            throws IllegalRequestArgumentException {
        return createSpeciality(form);
    }

    /**
     * Displays the current speciality name for editing.
     *
     * @param id speciality identifier
     * @param saved whether to display the saved notice
     * @param duplicate whether to display the duplicate-name notice
     * @return the speciality edit view model
     */
    @Get("/specialities/{id}/edit")
    @View("vets/createOrUpdateSpecialityForm")
    public Map<String, Object> editSpecialityForm(@PathVariable
                                                  Integer id,
                                                  @QueryValue(defaultValue = "false") boolean saved,
                                                  @QueryValue(defaultValue = "false") boolean duplicate) {
        Speciality speciality = vetService.findSpecialityById(id);
        return specialityFormModel(formMapper.toSpecialityDto(speciality), false, saved, duplicate);
    }

    /**
     * Renames a speciality from the JSON form submission.
     *
     * @param id speciality identifier from the URL
     * @param form submitted speciality name
     * @return HTTP 200 with the updated speciality
     * @throws IllegalRequestArgumentException if another speciality has that name
     */
    @Post(value = "/specialities/{id}/edit", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public HttpResponse<SpecialityDto> updateSpecialityForm(@PathVariable Integer id, @Valid @Body SpecialityDto form)
            throws IllegalRequestArgumentException {
        return updateSpeciality(id, form);
    }

    /**
     * Reads a veterinarian with its ETag and specialities.
     *
     * @param id veterinarian identifier
     * @return HTTP 200 with the veterinarian and quoted ETag header
     */
    @Get("/{id}")
    public HttpResponse<VetDto> readVet(@PathVariable Integer id) {
        return response(vetService.findById(id), HttpStatus.OK);
    }

    /**
     * Creates a veterinarian, resolving assignments against stored specialities.
     *
     * @param vetDto submitted names and speciality references
     * @return HTTP 201 with the saved veterinarian and ETag
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    @Post(consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<VetDto> createVet(@Valid @Body VetDto vetDto) throws IllegalRequestArgumentException {
        Vet saved = vetService.create(formMapper.toVet(vetDto));
        return response(saved, HttpStatus.CREATED);
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
    public HttpResponse<SpecialityDto> createSpeciality(@Valid @Body SpecialityDto request) throws IllegalRequestArgumentException {
        Speciality saved = vetService.createSpeciality(request);
        return HttpResponse.status(HttpStatus.CREATED).body(formMapper.toSpecialityDto(saved));
    }

    /**
     * Renames a speciality while preserving its assignments.
     *
     * @param id speciality identifier from the URL
     * @param request submitted speciality name
     * @return HTTP 200 with the updated speciality
     * @throws IllegalRequestArgumentException if another speciality has that name
     */
    @Put(value = "/specialities/{id}", consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<SpecialityDto> updateSpeciality(@PathVariable Integer id,
                                                        @Valid @Body SpecialityDto request) throws IllegalRequestArgumentException {
        Speciality updated = vetService.updateSpeciality(id, request);
        return HttpResponse.ok(formMapper.toSpecialityDto(updated));
    }

    /**
     * Updates a veterinarian using the ETag supplied in {@code If-Match}.
     *
     * @param id veterinarian identifier from the URL
     * @param ifMatch the current ETag, optionally enclosed in double quotes
     * @param request new names and speciality references
     * @return HTTP 200 with the updated veterinarian and ETag
     * @throws MissingEtagException if the header is absent or blank
     * @throws StaleEtagException if the veterinarian has changed
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    @Put(value = "/{id}", consumes = MediaType.APPLICATION_JSON)
    public HttpResponse<VetDto> updateVet(@PathVariable Integer id,
                                          @Nullable @Header("If-Match") String ifMatch,
                                          @Valid @Body VetDto request) throws MissingEtagException, StaleEtagException, IllegalRequestArgumentException {
        Vet current = vetService.findById(id);
        String etag = requiredEtag(ifMatch);
        VetDto update = new VetDto(request.id(), request.firstName(), request.lastName(),
                request.specialities(), etag);
        return response(vetService.update(current, update), HttpStatus.OK);
    }

    /**
     * Deletes a veterinarian using the ETag supplied in {@code If-Match}.
     *
     * @param id veterinarian identifier
     * @param ifMatch the current ETag, optionally enclosed in double quotes
     * @throws MissingEtagException if the header is absent or blank
     * @throws StaleEtagException if the veterinarian has changed
     */
    @Delete("/{id}")
    @Status(NO_CONTENT)
    public void deleteVet(@PathVariable Integer id,
                          @Nullable @Header("If-Match") String ifMatch) throws StaleEtagException, MissingEtagException {
        vetService.delete(id, requiredEtag(ifMatch));
    }

    private HttpResponse<VetDto> response(Vet vet, HttpStatus status) {
        return HttpResponse.status(status)
                .header("ETag", quote(vet.etag()))
                .header("Cache-Control", "no-store")
                .body(formMapper.toVetDto(vet));
    }

    private Map<String, Object> vetFormModel(VetForm form, boolean isNew, boolean conflict, boolean saved) {
        return Map.of("vetForm", form,
                "specialities", vetService.findSpecialities(),
                "isNew", isNew,
                "conflict", conflict,
                "saved", saved);
    }

    private static Map<String, Object> specialityFormModel(SpecialityDto form,
                                                           boolean isNew,
                                                           boolean saved,
                                                           boolean duplicate) {
        return Map.of("specialityForm", form, "isNew", isNew, "saved", saved, "duplicate", duplicate);
    }


    private static String requiredEtag(String ifMatch) throws MissingEtagException {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new MissingEtagException("Send the ETag from the loaded vet in the If-Match header.");
        }
        return ifMatch.trim();
    }

    private static String quote(String etag) {
        if (etag == null || etag.isBlank()) {
            throw new IllegalStateException("Micronaut Data did not return the generated vet ETag.");
        }
        return "\"" + etag + "\"";
    }

}
