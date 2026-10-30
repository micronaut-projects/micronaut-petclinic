package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.dto.LoyaltyRedemptionResponse;
import io.micronaut.samples.petclinic.dto.OwnerLoyaltySummary;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Disables loyalty persistence when no profile-specific loyalty service is available.
 * This includes non-Oracle profiles and Oracle Deep Data Security. Account lookups and
 * redemption return {@code null}; collection results and summaries are empty.
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
    @Nullable
    public OwnerLoyaltyAccount createAccount(Owner owner) {
        return null;
    }

    @Override
    @Nullable
    public OwnerLoyaltyAccount findOrCreateOwnerLoyaltyAccount(Owner owner) {
        return null;
    }

    @Override
    @Nullable
    public OwnerLoyaltyAccount findOrCreateOwnerLoyaltyAccount(Long id) {
        return null;
    }

    @Override
    public Set<OwnerLoyaltyAccount> findOwnerLoyaltyAccounts(Collection<Owner> owners) {
        return Set.of();
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
    public void reserveDecrementPoints(OwnerLoyaltyAccount account, long pointsCost) {
    }

    @Override
    public OwnerLoyaltySummary summary(OwnerLoyaltyAccount account) {
        return new OwnerLoyaltySummary(null, 0, 0, 0, List.of());
    }

    @Override
    @Nullable
    public LoyaltyRedemptionResponse redeem(OwnerLoyaltyAccount account) {
        return null;
    }
}
