package io.micronaut.samples.petclinic.oracle.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import static io.micronaut.data.annotation.Relation.Kind.MANY_TO_ONE;

/**
 * A recorded reward issued in exchange for loyalty points.
 *
 * @param id the generated database ID, or {@code null} before insertion
 * @param ownerLoyaltyAccount the account that funded the reward
 * @param rewardCode stable code identifying the reward
 * @param pointsSpent points deducted for the reward
 * @param createdAt server-local creation time without a time zone
 */
@MappedEntity("OWNER_REWARD_VOUCHERS")
public record OwnerRewardVoucher(
        @Id
        @GeneratedValue
        Long id,

        @Relation(MANY_TO_ONE)
        @MappedProperty("OWNER_LOYALTY_ACCOUNT_ID")
        @NotNull
        OwnerLoyaltyAccount ownerLoyaltyAccount,

        @MappedProperty("REWARD_CODE")
        @NotNull
        String rewardCode,

        @MappedProperty("POINTS_SPENT")
        @NotNull
        Long pointsSpent,

        @MappedProperty("CREATED_AT")
        @NotNull
        LocalDateTime createdAt
) {
}
