package io.micronaut.samples.petclinic.service.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.exceptions.DataIntegrityViolationException;
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
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NullMarked;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

/**
 * Loyalty persistence for the regular Oracle profile, excluding Oracle Deep Data Security.
 * Point credits join the visit transaction. Reward redemption uses a separate transaction
 * bean so the debit and voucher commit before the response balance is reloaded.
 * Account lookup creates a zero-balance account as a fallback when one is missing.
 */
@Singleton
@NullMarked
@Requires(env = "oracle", notEnv = "oracle-deepsec")
public class OracleOwnerLoyaltyService implements OwnerLoyaltyService {

    /** Fixed number of points spent per reward voucher. */
    public static final long REWARD_COST = 100L;
    /** Number of points earned for each newly inserted visit. */
    public static final long POINTS_PER_VISIT = 25;
    private static final String POINTS_CHECK_CONSTRAINT = "CK_OWNER_LOYALTY_ACCOUNT_POINTS_GE_0";

    private final OwnerLoyaltyAccountRepository ownerLoyaltyAccountRepository;
    private final OwnerRewardVoucherRepository voucherRepository;
    private final LoyaltyRedemptionTransaction redemptionTransaction;

    /**
     * Creates the Oracle loyalty service.
     * @param ownerLoyaltyAccountRepository account lookups, provisioning, and reservations
     * @param voucherRepository voucher lookup and deletion operations
     * @param redemptionTransaction independent transaction for issuing a funded voucher
     */
    public OracleOwnerLoyaltyService(OwnerLoyaltyAccountRepository ownerLoyaltyAccountRepository,
                                     OwnerRewardVoucherRepository voucherRepository,
                                     LoyaltyRedemptionTransaction redemptionTransaction) {
        this.ownerLoyaltyAccountRepository = ownerLoyaltyAccountRepository;
        this.voucherRepository = voucherRepository;
        this.redemptionTransaction = redemptionTransaction;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    @Transactional
    public OwnerLoyaltyAccount createAccount(Owner owner) {
        return findOrCreateOwnerLoyaltyAccount(owner);
    }

    @Override
    @Transactional
    public OwnerLoyaltyAccount findOrCreateOwnerLoyaltyAccount(Owner owner) {
        return ownerLoyaltyAccountRepository.findByOwner(owner)
                .orElseGet(() -> ownerLoyaltyAccountRepository.insert(new OwnerLoyaltyAccount(owner)));
    }

    @Override
    public OwnerLoyaltyAccount findOrCreateOwnerLoyaltyAccount(Long id) throws NotFoundEntityException {
        return ownerLoyaltyAccountRepository.findById(id).orElseThrow(NotFoundEntityException::new);
    }

    @Override
    public Set<OwnerLoyaltyAccount> findOwnerLoyaltyAccounts(Collection<Owner> owners) {
        return ownerLoyaltyAccountRepository.findAllByOwnerIn(owners);
    }

    @Override
    @Transactional
    public void ensureAccounts(Collection<Owner> owners) {
        for (Owner owner : owners) {
            findOrCreateOwnerLoyaltyAccount(owner);
        }
    }

    @Override
    @Transactional
    public void awardVisit(Owner owner) {
        OwnerLoyaltyAccount account = findOrCreateOwnerLoyaltyAccount(owner);
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
    public void reserveDecrementPoints(OwnerLoyaltyAccount account, long pointsCost) throws IllegalRequestArgumentException {
        if (pointsCost <= 0) {
            throw new IllegalRequestArgumentException("The points cost must be positive.");
        }
        if (ownerLoyaltyAccountRepository.reserveDecrementPoints(account.id(), pointsCost) != 1) {
            throw new IllegalStateException("The owner's loyalty account could not be debited.");
        }
    }

    @Override
    public OwnerLoyaltySummary summary(OwnerLoyaltyAccount account) {
        return new OwnerLoyaltySummary(account.owner().id(), account.points(), POINTS_PER_VISIT,
                REWARD_COST, account.vouchers().stream()
                        .sorted(Comparator.comparing(OwnerRewardVoucher::createdAt)
                                .thenComparing(OwnerRewardVoucher::id).reversed())
                        .limit(10)
                        .map(OracleOwnerLoyaltyService::toView).toList());
    }

    @Override
    public LoyaltyRedemptionResponse redeem(OwnerLoyaltyAccount account) throws IllegalRequestArgumentException {
        final OwnerRewardVoucher voucher;
        try {
            voucher = redemptionTransaction.redeem(account, REWARD_COST);
        } catch (DataIntegrityViolationException exception) {
            if (isInsufficientPointsConstraint(exception)) {
                throw new InsufficientLoyaltyPointsException();
            }
            throw exception;
        }
        // The transaction proxy has committed the reservation before this balance is read.
        long points = ownerLoyaltyAccountRepository.findById(account.id()).orElseThrow().points();
        return new LoyaltyRedemptionResponse(points, toView(voucher));
    }

    private static OwnerLoyaltySummary.Voucher toView(OwnerRewardVoucher voucher) {
        return new OwnerLoyaltySummary.Voucher("PCR-%08d".formatted(voucher.id()), voucher.rewardCode(),
                voucher.pointsSpent(), voucher.createdAt());
    }

    private static boolean isInsufficientPointsConstraint(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sqlException
                    && sqlException.getErrorCode() == 2290
                    && sqlException.getMessage() != null
                    && sqlException.getMessage().toUpperCase(Locale.ROOT).contains(POINTS_CHECK_CONSTRAINT)) {
                return true;
            }
        }
        return false;
    }

}
