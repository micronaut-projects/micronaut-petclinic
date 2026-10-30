package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.dto.LoyaltyRedemptionResponse;
import io.micronaut.samples.petclinic.dto.OwnerLoyaltySummary;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * Loyalty hooks shared by the owner and visit flows across database profiles.
 * The regular Oracle implementation persists accounts and vouchers. The fallback
 * implementation disables loyalty: account and redemption methods return {@code null},
 * collection lookups return empty sets, and persistence hooks do nothing.
 */
@NullMarked
public interface OwnerLoyaltyService {

    /** @return whether this profile persists loyalty accounts and rewards */
    boolean isEnabled();

    /**
     * Ensures an owner has an account without changing an existing balance.
     * @param owner the persisted owner
     * @return the existing or newly created account, or {@code null} when loyalty is disabled
     */
    @Nullable OwnerLoyaltyAccount createAccount(Owner owner);

    /**
     * Looks up an account by owner, creating a zero-balance account if missing.
     * This is a fallback; normal owner creation already initializes the account.
     * @param owner the persisted owner
     * @return the owner's account, or {@code null} when loyalty is disabled
     */
    @Nullable OwnerLoyaltyAccount findOrCreateOwnerLoyaltyAccount(Owner owner);

    /**
     * Looks up an existing account by its account ID. This overload does not create an account.
     * @param id the loyalty account ID, not the owner ID
     * @return the account, or {@code null} when loyalty is disabled
     * @throws NotFoundEntityException if loyalty is enabled and the account does not exist
     */
    @Nullable OwnerLoyaltyAccount findOrCreateOwnerLoyaltyAccount(Long id) throws NotFoundEntityException;

    /**
     * Finds existing accounts without creating missing ones.
     * @param owners the owners to look up
     * @return matching accounts, or an empty set when loyalty is disabled
     */
    Set<OwnerLoyaltyAccount> findOwnerLoyaltyAccounts(Collection<Owner> owners);

    /**
     * Creates missing zero-balance accounts without backfilling historical visit credits.
     * @param owners the persisted owners to reconcile
     */
    void ensureAccounts(Collection<Owner> owners);

    /**
     * Awards the fixed credit for one newly inserted visit, joining the caller's transaction.
     * Call only after inserting a visit; this hook does not inspect visits or deduplicate calls.
     * @param owner the persisted owner of the visited pet
     */
    void awardVisit(Owner owner);

    /**
     * Deletes an owner's vouchers and account, joining the caller's transaction.
     * @param owner the owner whose loyalty data is to be removed
     */
    void deleteOwnerData(Owner owner);

    /**
     * Reserves a points debit without issuing a voucher; use {@link #redeem} for rewards.
     * The debit becomes visible in the account balance when its transaction commits.
     * @param account the persisted account
     * @param pointsCost the positive number of points to debit
     * @throws IllegalRequestArgumentException if loyalty is enabled and the cost is not positive
     */
    void reserveDecrementPoints(OwnerLoyaltyAccount account, long pointsCost) throws IllegalRequestArgumentException;

    /**
     * Builds a summary from the supplied account and its fetched vouchers, without further queries.
     * Includes up to ten vouchers, ordered by creation time and ID descending.
     * @param account the already loaded account, including its voucher collection
     * @return the account summary, or an empty summary when loyalty is disabled
     */
    OwnerLoyaltySummary summary(OwnerLoyaltyAccount account);

    /**
     * Redeems the fixed-cost reward and reloads the balance after redemption commits.
     * The Oracle implementation uses an independent transaction for the debit and voucher;
     * failure rolls back both. The supplied account must already be committed.
     * @param account the persisted account funding the reward
     * @return the committed voucher and subsequently read balance, or {@code null} when disabled
     * @throws IllegalRequestArgumentException if the configured reward cost is not positive
     * @throws InsufficientLoyaltyPointsException if the points reservation is rejected for insufficient funds
     */
    @Nullable LoyaltyRedemptionResponse redeem(OwnerLoyaltyAccount account) throws IllegalRequestArgumentException;
}
