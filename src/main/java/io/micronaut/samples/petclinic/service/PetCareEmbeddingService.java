package io.micronaut.samples.petclinic.service;

import io.micronaut.data.model.vector.FloatVector;

/**
 * Converts pet-care text into the vector stored for semantic retrieval.
 */
public interface PetCareEmbeddingService {

    /**
     * Embeds text into the configured vector space.
     *
     * @param text source text
     * @return a fixed-size vector
     */
    FloatVector embed(String text);
}
