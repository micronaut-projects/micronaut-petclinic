package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.MessageSource;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.server.util.locale.HttpLocaleResolver;
import io.micronaut.samples.petclinic.dto.LoyaltyRedemptionResponse;
import io.micronaut.samples.petclinic.dto.OwnerLoyaltySummary;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.samples.petclinic.service.InsufficientLoyaltyPointsException;
import io.micronaut.samples.petclinic.service.OwnerLoyaltyService;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.Map;
import java.util.Objects;

/**
 * Loyalty JSON endpoints for the regular Oracle profile, excluding Oracle Deep Data Security.
 * Missing owners return HTTP 404; rejected points reservations return HTTP 409.
 */
@Controller("/owners/{ownerId}/loyalty")
@Requires(env = "oracle", notEnv = "oracle-deepsec")
@Secured(SecurityRule.IS_ANONYMOUS)
public class OwnerLoyaltyController {

    private final OwnerLoyaltyService ownerLoyaltyService;
    private final ClinicService clinicService;
    private final MessageSource messageSource;
    private final HttpLocaleResolver localeResolver;

    /**
     * Creates the loyalty endpoints with their owner and reward services.
     * @param ownerLoyaltyService loyalty provisioning, summaries, and redemption
     * @param clinicService owner lookup operations
     * @param messageSource the application translation bundles
     * @param localeResolver the resolver for the locale cookie and request headers
     */
    public OwnerLoyaltyController(OwnerLoyaltyService ownerLoyaltyService,
                                  ClinicService clinicService,
                                  MessageSource messageSource,
                                  HttpLocaleResolver localeResolver) {
        this.ownerLoyaltyService = ownerLoyaltyService;
        this.clinicService = clinicService;
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    /**
     * Returns the balance and up to ten recent vouchers, provisioning a zero-balance account if needed.
     * @param ownerId the owner ID, not the loyalty account ID
     * @return the current loyalty summary
     * @throws NotFoundEntityException if the owner does not exist
     */
    @Get
    public OwnerLoyaltySummary getLoyalty(@PathVariable Integer ownerId) throws NotFoundEntityException {
        Owner owner = clinicService.findOwnerById(ownerId);
        OwnerLoyaltyAccount ownerLoyaltyAccount = ownerLoyaltyService.findOrCreateOwnerLoyaltyAccount(owner);
        return ownerLoyaltyService.summary(Objects.requireNonNull(ownerLoyaltyAccount));
    }

    /**
     * Issues one fixed-cost reward voucher and returns HTTP 201 after redemption commits.
     * No request body is required; sufficient points are enforced by Oracle's reservation constraint.
     * @param ownerId the owner ID, not the loyalty account ID
     * @return the issued voucher and balance read after commit
     * @throws NotFoundEntityException if the owner does not exist
     * @throws IllegalRequestArgumentException if the configured reward cost is not positive
     * @throws InsufficientLoyaltyPointsException if the reward cannot be funded
     */
    @Post("/redemptions")
    @Status(HttpStatus.CREATED)
    public LoyaltyRedemptionResponse redeem(@PathVariable Integer ownerId) throws NotFoundEntityException, IllegalRequestArgumentException {
        Owner owner = clinicService.findOwnerById(ownerId);
        OwnerLoyaltyAccount ownerLoyaltyAccount = ownerLoyaltyService.findOrCreateOwnerLoyaltyAccount(owner);
        return ownerLoyaltyService.redeem(Objects.requireNonNull(ownerLoyaltyAccount));
    }

    /**
     * Reports an unfunded reward as HTTP 409 with a stable code and localized message.
     * @param request the request supplying the preferred locale
     * @param error the rejected reward reservation
     * @return the conflict response with code {@code INSUFFICIENT_LOYALTY_POINTS}
     */
    @Error(exception = InsufficientLoyaltyPointsException.class)
    public HttpResponse<Map<String, String>> insufficientLoyaltyPoints(HttpRequest<?> request,
                                                                      InsufficientLoyaltyPointsException error) {
        String localizedMessage = messageSource.getMessage("loyalty.insufficientPoints",
                localeResolver.resolveOrDefault(request)).orElse(error.getMessage());
        return HttpResponse.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "INSUFFICIENT_LOYALTY_POINTS", "message", localizedMessage));
    }

}
