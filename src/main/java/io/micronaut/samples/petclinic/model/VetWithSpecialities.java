package io.micronaut.samples.petclinic.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;

/**
 * Read projection for veterinarians and their aggregated speciality rows.
 *
 * @param id the vet identifier
 * @param firstName the vet's first name
 * @param lastName the vet's last name
 * @param specialityRows pipe-delimited speciality rows in {@code id:name} form
 */
@Introspected
public record VetWithSpecialities(
        Integer id,
        String firstName,
        String lastName,
        @Nullable String specialityRows
) {
}
