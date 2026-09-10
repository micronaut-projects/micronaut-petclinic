package io.micronaut.samples.petclinic.service;

import io.micronaut.data.model.vector.FloatVector;

import java.util.Optional;

/**
 * Provides the fixed vectors used for semantic retrieval.
 */
public interface PetCareEmbeddingService {

    /**
     * Returns the vector for text that is present in the checked-in catalog.
     *
     * @param text source text
     * @return a fixed-size vector
     */
    FloatVector embed(String text);

    /**
     * Finds a cataloged query vector without calculating a new embedding.
     *
     * @param text query text
     * @return the precomputed vector, or empty when this query is not cataloged
     */
    Optional<FloatVector> find(String text);
}
