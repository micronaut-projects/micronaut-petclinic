package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.views.View;
import java.util.Collection;
import java.util.Map;

/**
 * Controller for veterinarian-related operations.
 * Displays the list of vets with their specialties.
 */
@Controller("/vets")
public class VetController {

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
    @Get("/html")
    @View("vets/vetList")
    public Map<String, Object> showResourcesVetList() {
        return showVetList();
    }

    /**
     * Return the list of veterinarians as JSON.
     * @return collection of vets in JSON format
     */
    @Get("/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Vet> showResourcesVetListJson() {
        return clinicService.findAllVets();
    }

    /**
     * Wrapper for a list of vets for XML/JSON serialization.
     *
     * @param vetList the vets to expose
     */
    public record Vets(Collection<Vet> vetList) {
    }
}
