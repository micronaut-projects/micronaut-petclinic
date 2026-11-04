package io.micronaut.samples.petclinic.service.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import io.micronaut.samples.petclinic.oracle.model.OwnerRewardVoucher;
import io.micronaut.samples.petclinic.repository.oracle.OwnerLoyaltyAccountRepository;
import io.micronaut.samples.petclinic.repository.oracle.OwnerRewardVoucherRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import java.time.LocalDateTime;

import static jakarta.transaction.Transactional.TxType.*;

/**
 * Issues reward vouchers by reserving points and inserting the voucher atomically.
 * This separate bean makes the independent transaction boundary explicit: the debit and
 * voucher commit before returning to {@link OracleOwnerLoyaltyService}, which then re-reads
 * the balance. Oracle applies reserved points changes only at commit.
 */
@Singleton
@Requires(env = "oracle", notEnv = "oracle-deepsec")
public class LoyaltyRedemptionService {

    private final OwnerLoyaltyAccountRepository accountRepository;
    private final OwnerRewardVoucherRepository voucherRepository;

    /**
     * Creates the redemption service with its persistence dependencies.
     * @param accountRepository points reservation operations
     * @param voucherRepository voucher persistence operations
     */
    public LoyaltyRedemptionService(OwnerLoyaltyAccountRepository accountRepository,
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
     * @throws NotFoundEntityException if the account no longer exists when the debit is reserved
     */
    @Transactional(value = REQUIRES_NEW, rollbackOn = Throwable.class)
    public OwnerRewardVoucher redeem(OwnerLoyaltyAccount account, long pointsCost)
            throws IllegalRequestArgumentException, NotFoundEntityException {
        if (pointsCost <= 0) {
            throw new IllegalRequestArgumentException("The points cost must be positive.");
        }
        if (accountRepository.reserveDecrementPoints(account.id(), pointsCost) != 1) {
            throw new NotFoundEntityException();
        }
        return voucherRepository.save(new OwnerRewardVoucher(null, account, "PET_CARE_REWARD",
                pointsCost, LocalDateTime.now()));
    }
}
