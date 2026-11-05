package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Issued voucher and balance reloaded after the redemption transaction commits.
 * Other transactions may commit before the balance is read.
 * @param points the committed balance observed after redemption
 * @param voucher the issued reward voucher
 */
@Serdeable
public record LoyaltyRedemptionResponse(
        long points,
        OwnerLoyaltySummary.Voucher voucher) {
}
