package io.micronaut.samples.petclinic.repository.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.GenericRepository;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Inserts and looks up Oracle loyalty accounts and applies point deltas by primary key.
 * General save/update operations are deliberately absent because points are reservable.
 * Reservation deltas are applied to the stored balance at transaction commit.
 */
@Requires(env = "oracle", notEnv = "oracle-deepsec")
@JdbcRepository(dialect = Dialect.ORACLE)
@NullMarked
public interface OwnerLoyaltyAccountRepository extends GenericRepository<OwnerLoyaltyAccount, Long> {

    /**
     * Inserts a new account; callers must handle provisioning an account only once per owner.
     * @param account the new account with its initial points balance
     * @return the inserted account with its generated ID
     */
    OwnerLoyaltyAccount insert(OwnerLoyaltyAccount account);

    /**
     * Finds an owner's account with its owner and vouchers fetched.
     * Voucher account references are fetched to populate their required record components.
     * @param owner the persisted owner
     * @return the account, if present
     */
    @Join(value = "owner", type = Join.Type.LEFT_FETCH)
    @Join(value = "vouchers", type = Join.Type.LEFT_FETCH)
    @Join(value = "vouchers.ownerLoyaltyAccount", type = Join.Type.LEFT_FETCH)
    @Join(value = "vouchers.ownerLoyaltyAccount.owner", type = Join.Type.LEFT_FETCH)
    Optional<OwnerLoyaltyAccount> findByOwner(Owner owner);

    /**
     * Finds an account by its own ID with its owner and vouchers fetched.
     * Voucher account references are fetched to populate their required record components.
     * @param id the loyalty account ID
     * @return the account, if present
     */
    @Join(value = "owner", type = Join.Type.LEFT_FETCH)
    @Join(value = "vouchers", type = Join.Type.LEFT_FETCH)
    @Join(value = "vouchers.ownerLoyaltyAccount", type = Join.Type.LEFT_FETCH)
    @Join(value = "vouchers.ownerLoyaltyAccount.owner", type = Join.Type.LEFT_FETCH)
    Optional<OwnerLoyaltyAccount> findById(Long id);

    /**
     * Deletes an owner's account; associated vouchers must be deleted first.
     * @param owner the owner whose account is to be deleted
     */
    void deleteByOwner(Owner owner);

    /**
     * Reserves an increase to an account's points balance.
     * @param accountId the loyalty account ID
     * @param points the positive points increment
     * @return the number of accounts matched, normally one
     */
    long reserveIncrementPoints(@Id Long accountId, Long points);

    /**
     * Reserves a decrease, subject to the database's nonnegative balance constraint.
     * @param accountId the loyalty account ID
     * @param points the positive points decrement
     * @return the number of accounts matched, normally one
     */
    long reserveDecrementPoints(@Id Long accountId, Long points);

    /**
     * Finds existing accounts for the supplied owners without creating missing accounts.
     * @param owners the persisted owners
     * @return matching accounts with their owners fetched
     */
    @Join(value = "owner", type = Join.Type.LEFT_FETCH)
    Set<OwnerLoyaltyAccount> findAllByOwnerIn(Collection<Owner> owners);
}
