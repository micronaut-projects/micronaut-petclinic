package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Request to confirm or cancel a suspended sessionless booking.
 *
 * <p>The opaque transaction ID travels in the JSON body, never in the URL.</p>
 *
 * @param appointmentId identifier of the appointment being completed
 * @param transactionId opaque handle returned by the hold request
 */
@Serdeable
public record SessionlessBookingRequest(
        @NotNull @Positive Integer appointmentId,
        @NotBlank @Size(max = 86) String transactionId
) {
}
