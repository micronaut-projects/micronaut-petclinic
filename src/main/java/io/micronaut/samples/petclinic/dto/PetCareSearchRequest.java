package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * JSON request for a pet-care semantic search.
 *
 * @param query natural-language search text
 */
@Serdeable
public record PetCareSearchRequest(String query) {
}
