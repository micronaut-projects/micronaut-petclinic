package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.dto.LoyaltyRedemptionResponse;
import io.micronaut.samples.petclinic.dto.OwnerLoyaltySummary;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;

/**
 * Disables loyalty persistence when no profile-specific loyalty service is available.
 * This includes non-Oracle profiles and Oracle Deep Data Security. Account lookups and
 * redemption report not found; persistence hooks do nothing and summaries are empty.
 */
@Singleton
@Requires(missingBeans = OwnerLoyaltyService.class)
@NullMarked
public class NoOpOwnerLoyaltyService implements OwnerLoyaltyService {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void createAccount(Owner owner) {
    }

    @Override
    public OwnerLoyaltyAccount findOwnerLoyaltyAccount(Owner owner) throws NotFoundEntityException {
        throw new NotFoundEntityException();
    }

    @Override
    public void ensureAccounts(Collection<Owner> owners) {
    }

    @Override
    public void awardVisit(Owner owner) {
    }

    @Override
    public void deleteOwnerData(Owner owner) {
    }

    @Override
    public OwnerLoyaltySummary summary(OwnerLoyaltyAccount account) {
        return new OwnerLoyaltySummary(null, 0, 0, 0, List.of());
    }

    @Override
    public LoyaltyRedemptionResponse redeem(OwnerLoyaltyAccount account) throws NotFoundEntityException {
        throw new NotFoundEntityException();
    }
}
