package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.dto.LoyaltyRedemptionResponse;
import io.micronaut.samples.petclinic.dto.OwnerLoyaltySummary;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;

/**
 * Loyalty hooks shared by the owner and visit flows across database profiles.
 * The Oracle implementation persists accounts and vouchers. When loyalty is disabled,
 * persistence hooks do nothing, account lookup and redemption report not found,
 * and summaries are empty. Callers can check {@link #isEnabled()} before loading loyalty data.
 */
@NullMarked
public interface OwnerLoyaltyService {

    /** @return whether this profile persists loyalty accounts and rewards */
    boolean isEnabled();

    /**
     * Ensures an owner has an account without changing an existing balance.
     * @param owner the persisted owner
     */
    void createAccount(Owner owner);

    /**
     * Looks up an existing account by owner without creating one.
     * @param owner the persisted owner
     * @return the owner's account
     * @throws NotFoundEntityException if the account does not exist or loyalty is disabled
     */
    OwnerLoyaltyAccount findOwnerLoyaltyAccount(Owner owner) throws NotFoundEntityException;

    /**
     * Creates missing zero-balance accounts without backfilling historical visit credits.
     * @param owners the persisted owners to reconcile
     */
    void ensureAccounts(Collection<Owner> owners);

    /**
     * Awards the fixed credit for one newly inserted visit, joining the caller's transaction.
     * Creates a missing zero-balance account in that transaction before awarding the credit.
     * Call only after inserting a visit; this hook does not inspect visits or deduplicate calls.
     * @param owner the persisted owner of the visited pet
     * @throws IllegalStateException if loyalty is enabled and the account cannot be credited
     */
    void awardVisit(Owner owner);

    /**
     * Deletes an owner's vouchers and account, joining the caller's transaction.
     * @param owner the owner whose loyalty data is to be removed
     */
    void deleteOwnerData(Owner owner);

    /**
     * Builds a summary from the supplied account, querying its ten newest vouchers.
     * Voucher ordering by creation time and ID descending and limiting happen in the database.
     * @param account the already loaded account supplying the balance
     * @return the account summary, or an empty summary when loyalty is disabled
     */
    OwnerLoyaltySummary summary(OwnerLoyaltyAccount account);

    /**
     * Redeems the fixed-cost reward and reloads the balance after redemption commits.
     * The Oracle implementation uses an independent transaction for the debit and voucher;
     * failure rolls back both. The supplied account must already be committed.
     * @param account the persisted account funding the reward
     * @return the committed voucher and subsequently read balance
     * @throws IllegalRequestArgumentException if the configured reward cost is not positive
     * @throws NotFoundEntityException if loyalty is disabled or the account disappears before the debit or balance lookup
     * @throws InsufficientLoyaltyPointsException if the points reservation is rejected for insufficient funds
     */
    LoyaltyRedemptionResponse redeem(OwnerLoyaltyAccount account)
            throws IllegalRequestArgumentException, NotFoundEntityException;
}
