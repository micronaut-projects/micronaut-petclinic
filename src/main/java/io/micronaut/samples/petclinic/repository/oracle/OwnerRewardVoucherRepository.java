package io.micronaut.samples.petclinic.repository.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.oracle.model.OwnerLoyaltyAccount;
import io.micronaut.samples.petclinic.oracle.model.OwnerRewardVoucher;

import java.util.List;

/** Oracle persistence operations for issued reward vouchers. */
@Requires(env = "oracle", notEnv = "oracle-deepsec")
@JdbcRepository(dialect = Dialect.ORACLE)
public interface OwnerRewardVoucherRepository extends CrudRepository<OwnerRewardVoucher, Long> {

    /**
     * Returns at most ten vouchers ordered by creation time descending, then ID descending.
     * Fetches each voucher's account and owner to populate the required associations.
     * @param account the persisted account
     * @return the newest vouchers, or an empty list
     */
    @Join(value = "ownerLoyaltyAccount", type = Join.Type.LEFT_FETCH)
    @Join(value = "ownerLoyaltyAccount.owner", type = Join.Type.LEFT_FETCH)
    List<OwnerRewardVoucher> findTop10ByOwnerLoyaltyAccountOrderByCreatedAtDescAndIdDesc(OwnerLoyaltyAccount account);

    /**
     * Deletes all vouchers for an account before the account itself is removed.
     * @param account the persisted account
     */
    void deleteByOwnerLoyaltyAccount(OwnerLoyaltyAccount account);
}
