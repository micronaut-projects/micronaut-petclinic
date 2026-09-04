package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.samples.petclinic.dto.VisitDto;
import io.micronaut.samples.petclinic.dto.VisitSearchCriteria;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.views.View;
import org.jspecify.annotations.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Displays and filters visits.
 */
@Controller("/visits")
@Requires(env = "oracle")
public class VisitSearchController {

    private final ClinicService clinicService;

    /**
     * Creates the visit search controller.
     *
     * @param clinicService the clinic service facade
     */
    public VisitSearchController(ClinicService clinicService) {
        this.clinicService = clinicService;
    }

    /**
     * Displays visits search view.
     *
     * @return the visit search page model
     */
    @Get
    @View("visits/searchVisits")
    public Map<String, Object> searchView() {
        return Map.of();
    }

    /**
     * Search for matching visites.
     * @param fromDate
     * @param toDate
     * @param maxDurationMinutes maximum duration in minutes
     * @param maxFollowUpMonths maximum follow-up period in months
     * @return list of matching visits
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/search")
    public List<VisitDto> search(@QueryValue @NonNull LocalDate fromDate,
                                 @QueryValue @NonNull LocalDate toDate,
                                 @QueryValue(defaultValue = "120" ) Integer maxDurationMinutes,
                                 @QueryValue(defaultValue = "12") Integer maxFollowUpMonths) {
        VisitSearchCriteria criteria = new VisitSearchCriteria(fromDate, toDate, maxDurationMinutes, maxFollowUpMonths);
        return VisitDto.from(clinicService.searchVisits(criteria));
    }
}
