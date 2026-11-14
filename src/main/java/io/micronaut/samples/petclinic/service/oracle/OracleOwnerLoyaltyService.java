package io.micronaut.samples.petclinic.service.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.dto.LoyaltyRedemptionResponse;
import io.micronaut.samples.petclinic.dto.OwnerLoyaltySummary;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import io.micronaut.samples.petclinic.oracle.model.OwnerRewardVoucher;
import io.micronaut.samples.petclinic.repository.oracle.OwnerLoyaltyAccountRepository;
import io.micronaut.samples.petclinic.repository.oracle.OwnerRewardVoucherRepository;
import io.micronaut.samples.petclinic.service.InsufficientLoyaltyPointsException;
import io.micronaut.samples.petclinic.service.OwnerLoyaltyService;
import io.micronaut.transaction.exceptions.TransactionSystemException;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NullMarked;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collection;

/**
 * Manages loyalty balances and reward vouchers using Oracle lock-free points reservations.
 * New visits earn {@value #POINTS_PER_VISIT} points in the visit transaction; each reward
 * spends {@value #REWARD_COST} points. Oracle's {@code CHECK (POINTS >= 0)} rejects
 * overspending, including pending debit reservations from concurrent transactions.
 * Oracle applies reserved balance changes at commit, so {@link LoyaltyRedemptionService}
 * commits the debit and voucher independently before this service reloads the response balance.
 * Accounts are provisioned during owner creation, startup reconciliation, or a new visit
 * when missing; lookups never insert.
 */
@Singleton
@NullMarked
@Requires(env = "oracle", notEnv = "oracle-deepsec")
public class OracleOwnerLoyaltyService implements OwnerLoyaltyService {

    /** Fixed number of points spent per reward voucher. */
    public static final long REWARD_COST = 100L;
    /** Number of points earned for each newly inserted visit. */
    public static final long POINTS_PER_VISIT = 25;

    private final OwnerLoyaltyAccountRepository ownerLoyaltyAccountRepository;
    private final OwnerRewardVoucherRepository voucherRepository;
    private final LoyaltyRedemptionService redemptionService;

    /**
     * Creates the Oracle loyalty service.
     * @param ownerLoyaltyAccountRepository account lookups, provisioning, and reservations
     * @param voucherRepository voucher lookup and deletion operations
     * @param redemptionService issues funded vouchers in an independent transaction
     */
    public OracleOwnerLoyaltyService(OwnerLoyaltyAccountRepository ownerLoyaltyAccountRepository,
                                     OwnerRewardVoucherRepository voucherRepository,
                                     LoyaltyRedemptionService redemptionService) {
        this.ownerLoyaltyAccountRepository = ownerLoyaltyAccountRepository;
        this.voucherRepository = voucherRepository;
        this.redemptionService = redemptionService;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    @Transactional
    public void createAccount(Owner owner) {
        ownerLoyaltyAccountRepository.findByOwner(owner)
                .orElseGet(() -> ownerLoyaltyAccountRepository.insert(new OwnerLoyaltyAccount(owner)));
    }

    @Override
    public OwnerLoyaltyAccount findOwnerLoyaltyAccount(Owner owner) throws NotFoundEntityException {
        return ownerLoyaltyAccountRepository.findByOwner(owner)
                .orElseThrow(NotFoundEntityException::new);
    }

    @Override
    @Transactional
    public void ensureAccounts(Collection<Owner> owners) {
        for (Owner owner : owners) {
            createAccount(owner);
        }
    }

    @Override
    @Transactional
    public void awardVisit(Owner owner) {
        OwnerLoyaltyAccount account = ownerLoyaltyAccountRepository.findByOwner(owner)
                .orElseGet(() -> ownerLoyaltyAccountRepository.insert(new OwnerLoyaltyAccount(owner)));
        if (ownerLoyaltyAccountRepository.reserveIncrementPoints(account.id(), POINTS_PER_VISIT) != 1) {
            throw new IllegalStateException("The owner's loyalty account could not be credited.");
        }
    }

    @Override
    @Transactional
    public void deleteOwnerData(Owner owner) {
        ownerLoyaltyAccountRepository.findByOwner(owner).ifPresent(voucherRepository::deleteByOwnerLoyaltyAccount);
        ownerLoyaltyAccountRepository.deleteByOwner(owner);
    }

    @Override
    public OwnerLoyaltySummary summary(OwnerLoyaltyAccount account) {
        return new OwnerLoyaltySummary(account.owner().id(), account.points(), POINTS_PER_VISIT,
                REWARD_COST, voucherRepository.findTop10ByOwnerLoyaltyAccountOrderByCreatedAtDescAndIdDesc(account)
                        .stream().map(OracleOwnerLoyaltyService::toView).toList());
    }

    /**
     * Redeems a reward and maps insufficient points reported during the reservation or its commit.
     * @param account the committed account funding the reward
     * @return the committed voucher and subsequently read balance
     * @throws IllegalRequestArgumentException if the configured reward cost is not positive
     * @throws NotFoundEntityException if the account disappears before the debit or balance lookup
     * @throws InsufficientLoyaltyPointsException if the nonnegative points constraint rejects the reward
     * @throws TransactionSystemException if the independent transaction fails for another reason
     */
    @Override
    public LoyaltyRedemptionResponse redeem(OwnerLoyaltyAccount account)
            throws IllegalRequestArgumentException, NotFoundEntityException {
        OwnerRewardVoucher voucher;
        try {
            voucher = redemptionService.redeem(account, REWARD_COST);
        } catch (TransactionSystemException exception) {
            // A racing reservation can violate the points check at commit, after the inner method returns.
            if (exception.getCause() instanceof SQLIntegrityConstraintViolationException violation
                    && violation.getErrorCode() == 2290
                    && violation.getMessage() != null
                    && violation.getMessage().contains("CK_OWNER_LOYALTY_ACCOUNT_POINTS_GE_0")) {
                throw new InsufficientLoyaltyPointsException();
            }
            throw exception;
        }
        // The transaction proxy has committed the reservation before this balance is read.
        long points = ownerLoyaltyAccountRepository.findPointsById(account.id())
                .orElseThrow(NotFoundEntityException::new);
        return new LoyaltyRedemptionResponse(points, toView(voucher));
    }

    private static OwnerLoyaltySummary.Voucher toView(OwnerRewardVoucher voucher) {
        return new OwnerLoyaltySummary.Voucher("PCR-%08d".formatted(voucher.id()), voucher.rewardCode(),
                voucher.pointsSpent(), voucher.createdAt());
    }

}
