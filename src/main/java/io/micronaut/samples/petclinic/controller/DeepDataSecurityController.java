package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Produces;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.repository.oracle.DeepSecOwnerRepository;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Browser and JSON surfaces for demonstrating database-enforced authorization.
 *
 * <p>These endpoints use the same {@link ClinicService} and Micronaut Data
 * repository path as the application. The difference users observe comes
 * from Oracle DATA GRANT policies, not from filtering this response in Java.</p>
 */
@Requires(env = "oracle-deepsec")
@Controller("/deepsec")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class DeepDataSecurityController {

    private final ClinicService clinicService;
    private final DeepSecOwnerRepository deepSecOwnerRepository;

    /**
     * Creates the Deep Data Security demo controller.
     *
     * @param clinicService the normal application service
     * @param deepSecOwnerRepository the Oracle-only elevated repository
     */
    public DeepDataSecurityController(ClinicService clinicService,
                                      DeepSecOwnerRepository deepSecOwnerRepository) {
        this.clinicService = clinicService;
        this.deepSecOwnerRepository = deepSecOwnerRepository;
    }

    /**
     * Renders the DeepSec showcase page using an ordinary Micronaut Data query.
     *
     * <p>Depending on the token's roles and context attributes, Oracle may
     * filter rows or return protected column values as {@code null}.</p>
     *
     * @param authentication the authenticated request principal
     * @return the page model containing the principal and Oracle-visible rows
    */
    @Get("/owners")
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Produces(MediaType.TEXT_HTML)
    @View("deepsec/owners")
    public Map<String, Object> ownersPage(Authentication authentication) {
        OwnerDataResponse data = loadOwners(authentication);
        return Map.of(
                "user", displayUser(authentication),
                "subject", data.user(),
                "roles", data.roles(),
                "owners", data.owners()
        );
    }

    /**
     * Lists owners as JSON for API and troubleshooting use.
     *
     * @param authentication the authenticated request principal
     * @return the principal and the rows visible to that principal
     */
    @Get("/owners.json")
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Produces(MediaType.APPLICATION_JSON)
    public OwnerDataResponse ownersJson(Authentication authentication) {
        return loadOwners(authentication);
    }

    private OwnerDataResponse loadOwners(Authentication authentication) {
        Collection<Owner> owners = clinicService.findAllOwners();
        return new OwnerDataResponse(
                authentication.getName(),
                List.copyOf(authentication.getRoles()),
                owners.stream().map(OwnerData::from).toList()
        );
    }

    private static String displayUser(Authentication authentication) {
        for (String attributeName : List.of("preferred_username", "upn", "email", "unique_name")) {
            Object value = authentication.getAttributes().get(attributeName);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return authentication.getName();
    }

    /**
     * Demonstrates a narrowly scoped {@link io.micronaut.security.annotation.RunAs}
     * operation. The support role is requested only while the repository
     * method executes.
     *
     * @param ownerId the owner id
     * @return the elevated result, or 404 when the owner is not visible
    */
    @Get("/owners/{ownerId}/support-contact")
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Secured("STAFF")
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<OwnerData> supportContact(@PathVariable Integer ownerId) {
        return deepSecOwnerRepository.findByIdWithSupport(ownerId)
                .map(owner -> HttpResponse.ok(OwnerData.from(owner)))
                .orElseGet(HttpResponse::notFound);
    }

    /**
     * Response containing the authenticated identity and database-visible rows.
     *
     * @param user the authenticated identity
     * @param roles the application roles extracted from the JWT
     * @param owners the rows returned by Oracle
     */
    @Serdeable
    public record OwnerDataResponse(String user, List<String> roles, List<OwnerData> owners) {
    }

    /**
     * A non-cyclic representation of an owner suitable for the demo API.
     * A protected Oracle column remains visible as {@code null}, making column
     * masking obvious without serializing the complete entity graph.
     *
     * @param id owner id
     * @param firstName owner first name
     * @param lastName owner last name
     * @param city owner city
     * @param address owner address
     * @param telephone owner telephone, possibly masked by Oracle
     * @param pets names of pets returned by the same secured que2ry graph
     */
    @Serdeable
    public record OwnerData(Integer id,
                            String firstName,
                            String lastName,
                            String city,
                            String address,
                            String telephone,
                            List<String> pets) {

        private static OwnerData from(Owner owner) {
            List<String> petNames = owner.pets() == null
                    ? List.of()
                    : owner.pets().stream()
                    .map(pet -> pet != null ? pet.name() : null)
                    .filter(Objects::nonNull)
                    .toList();
            return new OwnerData(
                    owner.id(),
                    owner.firstName(),
                    owner.lastName(),
                    owner.city(),
                    owner.address(),
                    owner.telephone(),
                    petNames
            );
        }
    }
}
