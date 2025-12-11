package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.views.View;
import java.util.*;

/**
 * Controller for veterinarian-related operations.
 * Displays the list of vets with their specialties.
 */
@Controller("/vets")
public class VetController {

    private final ClinicService clinicService;

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
        Map<String, Object> model = new HashMap<>();
        Collection<Vet> vets = clinicService.findAllVets();
        model.put("vets", vets);
        return model;
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
     * A wrapper class to hold the list of vets for XML/JSON serialization.
     */
    public static class Vets {
        private Collection<Vet> vetList;

        public Vets(Collection<Vet> vets) {
            this.vetList = vets;
        }

        public Collection<Vet> getVetList() {
            return vetList;
        }

        public void setVetList(Collection<Vet> vetList) {
            this.vetList = vetList;
        }
    }
}
