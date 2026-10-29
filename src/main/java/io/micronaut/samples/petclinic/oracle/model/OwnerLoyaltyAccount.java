package io.micronaut.samples.petclinic.oracle.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Index;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.annotation.Reservable;
import io.micronaut.samples.petclinic.model.Owner;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.NonNull;

import java.util.Set;

import static io.micronaut.data.annotation.Relation.Kind.ONE_TO_MANY;
import static io.micronaut.data.annotation.Relation.Kind.ONE_TO_ONE;

/**
 * Oracle loyalty balance kept outside the owner document and owner row.
 * Points are initialized on insert and subsequently changed only through reservation deltas.
 * The voucher collection is populated only when explicitly fetched; summaries query vouchers separately.
 *
 * @param id the generated account ID, or {@code null} before insertion
 * @param owner the one-to-one owner association
 * @param points the committed reservable balance represented by this record
 * @param vouchers the account's reward vouchers when explicitly fetched
 */
@MappedEntity("OWNER_LOYALTY_ACCOUNT")
@Index(name = "UK_OWNER_LOYALTY_ACCOUNT_OWNER", columns = "OWNER_ID", unique = true)
public record OwnerLoyaltyAccount(
        @Id
        @GeneratedValue
        Long id,

        @Relation(ONE_TO_ONE)
        @MappedProperty("OWNER_ID")
        @NotNull
        Owner owner,

        @Reservable
        @NotNull
        @PositiveOrZero
        @MappedProperty("POINTS")
        Long points,

        @Relation(value = ONE_TO_MANY,
                mappedBy = "ownerLoyaltyAccount",
                cascade = Relation.Cascade.ALL)
        @NonNull
        Set<OwnerRewardVoucher> vouchers
) {

    /**
     * Creates a zero-balance account for an existing owner.
     *
     * @param owner the owner
     */
    public OwnerLoyaltyAccount(Owner owner) {
        this(null, owner, 0L, Set.of());
    }
}
