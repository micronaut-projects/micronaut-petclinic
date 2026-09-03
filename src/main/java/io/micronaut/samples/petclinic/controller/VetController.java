package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.samples.petclinic.dto.VetForm;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.service.ClinicService;
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

/**
 * Controller for veterinarian-related operations.
 * Displays the list of vets with their specialities.
 */
@Controller("/vets")
public class VetController {

    private static final String VIEW_VET_FORM = "vets/createOrUpdateVetForm";

    private final ClinicService clinicService;

    /**
     * Creates the controller with the service facade.
     *
     * @param clinicService the facade used for vet lookups
     */
    public VetController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    /**
     * Display the list of veterinarians.
     * @return the vets list view
     */
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get
    @View("vets/vetList")
    public Map<String, Object> showVetList() {
        Collection<Vet> vets = clinicService.findAllVets();
        return Map.of("vets", vets);
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
     * Wrapper for a list of vets for XML/JSON serialization.
     *
     * @param vetList the vets to expose
     */
    public record Vets(Collection<Vet> vetList) {
    }
}
