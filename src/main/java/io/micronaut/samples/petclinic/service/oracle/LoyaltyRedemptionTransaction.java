package io.micronaut.samples.petclinic.service.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import io.micronaut.samples.petclinic.oracle.model.OwnerRewardVoucher;
import io.micronaut.samples.petclinic.repository.oracle.OwnerLoyaltyAccountRepository;
import io.micronaut.samples.petclinic.repository.oracle.OwnerRewardVoucherRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

/**
 * Atomically reserves points and inserts a reward voucher in an independent transaction.
 * Invoking this bean through its transaction proxy commits before returning to
 * {@link OracleOwnerLoyaltyService}, which then reloads the committed balance.
 */
@Singleton
@Requires(env = "oracle", notEnv = "oracle-deepsec")
public class LoyaltyRedemptionTransaction {

    private final OwnerLoyaltyAccountRepository accountRepository;
    private final OwnerRewardVoucherRepository voucherRepository;

    /**
     * Creates the reward transaction with its persistence dependencies.
     * @param accountRepository points reservation operations
     * @param voucherRepository voucher persistence operations
     */
    public LoyaltyRedemptionTransaction(OwnerLoyaltyAccountRepository accountRepository,
                                        OwnerRewardVoucherRepository voucherRepository) {
        this.accountRepository = accountRepository;
        this.voucherRepository = voucherRepository;
    }

    /**
     * Reserves a debit and records its voucher, rolling back both if either operation fails.
     * Suspends any caller transaction; the account must already be committed.
     * @param account the persisted account funding the reward
     * @param pointsCost the positive reward cost
     * @return the saved voucher; the transaction proxy commits before the caller receives it
     * @throws IllegalRequestArgumentException if the reward cost is not positive
     * @throws IllegalStateException if the account cannot be debited because it is missing
     */
    @Transactional(value = Transactional.TxType.REQUIRES_NEW, rollbackOn = Throwable.class)
    public OwnerRewardVoucher redeem(OwnerLoyaltyAccount account, long pointsCost) throws IllegalRequestArgumentException {
        if (pointsCost <= 0) {
            throw new IllegalRequestArgumentException("The points cost must be positive.");
        }
        if (accountRepository.reserveDecrementPoints(account.id(), pointsCost) != 1) {
            throw new IllegalStateException("The owner's loyalty account could not be debited.");
        }
        return voucherRepository.save(new OwnerRewardVoucher(null, account, "PET_CARE_REWARD",
                pointsCost, LocalDateTime.now()));
    }
}
