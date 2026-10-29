package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Current loyalty balance, earning rate, and newest vouchers returned by the Oracle endpoints.
 * @param ownerId the owner ID, or {@code null} in the disabled-profile summary
 * @param points the committed points balance read for this summary
 * @param pointsPerVisit the fixed credit for a newly inserted visit
 * @param rewardCost the fixed cost of one reward voucher
 * @param recentVouchers up to ten vouchers, ordered by creation time and ID descending
 */
@Serdeable
public record OwnerLoyaltySummary(
        Integer ownerId,
        long points,
        long pointsPerVisit,
        long rewardCost,
        List<Voucher> recentVouchers
) {

    /**
     * Copies the voucher list and normalizes a missing list to an empty one.
     * @param ownerId the owner ID
     * @param points the points balance
     * @param pointsPerVisit points earned per new visit
     * @param rewardCost points spent per reward
     * @param recentVouchers the newest vouchers, or {@code null} for an empty list
     */
    public OwnerLoyaltySummary {
        recentVouchers = recentVouchers != null ? List.copyOf(recentVouchers) : List.of();
    }

    /**
     * Public representation of an issued reward voucher.
     * @param reference the stable voucher reference, such as {@code PCR-00000001}
     * @param rewardCode the stable reward code, currently {@code PET_CARE_REWARD}
     * @param pointsSpent the points debited for this voucher
     * @param createdAt server-local creation time without a time zone
     */
    @Serdeable
    public record Voucher(String reference, String rewardCode, long pointsSpent, LocalDateTime createdAt) {
    }
}
