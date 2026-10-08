package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.model.vector.Vector;
import io.micronaut.data.model.vector.search.Score;
import io.micronaut.data.model.vector.search.ScoringFunction;
import io.micronaut.data.model.vector.search.SearchResults;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.PetCareChunk;

/**
 * Repository for vector-searchable pet-care chunks.
 * <p>
 * The Oracle-specific implementation is generated from this interface. The
 * derived method is intentionally kept here so the Micronaut Data vector
 * search feature is visible in the example.
 */
public interface PetCareChunkRepository extends CrudRepository<PetCareChunk, Integer> {

    /**
     * Finds the five nearest chunks to a query vector using the selected metric.
     *
     * @param embedding the query embedding
     * @param maxDistance maximum accepted distance
     * @param scoringFunction the Oracle vector scoring function
     * @return ranked vector search results
     */
    SearchResults<PetCareChunk> searchTop5ByEmbeddingNear(Vector embedding,
                                                          Score maxDistance,
                                                          ScoringFunction scoringFunction);
}
