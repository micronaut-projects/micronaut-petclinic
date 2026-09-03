package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.model.vector.search.Score;
import io.micronaut.data.model.vector.search.ScoringFunction;
import io.micronaut.data.model.vector.search.SearchResults;
import io.micronaut.samples.petclinic.dto.PetCareSearchResult;
import io.micronaut.samples.petclinic.model.PetCareChunk;
import io.micronaut.samples.petclinic.repository.PetCareChunkRepository;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Performs semantic retrieval against the Oracle pet-care chunk table.
 */
@Singleton
@Requires(env = "oracle")
public class PetCareKnowledgeService {

    private static final Score MAX_COSINE_DISTANCE = new Score(2.0);

    private final PetCareChunkRepository chunkRepository;
    private final PetCareEmbeddingService embeddingService;

    /**
     * Creates the knowledge search service.
     *
     * @param chunkRepository repository generated for Oracle
     * @param embeddingService checked-in vector catalog
     */
    public PetCareKnowledgeService(PetCareChunkRepository chunkRepository,
                                   PetCareEmbeddingService embeddingService) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
    }

    /**
     * Retrieves the five closest chunks for a cataloged natural-language query.
     *
     * @param query natural-language search text
     * @return ranked matching chunks
     */
    public List<PetCareSearchResult> search(@NonNull String query) {
        if (query.isBlank()) {
            return List.of();
        }

        var queryVector = embeddingService.find(query);
        if (queryVector.isEmpty()) {
            return List.of();
        }

        SearchResults<PetCareChunk> results =
                chunkRepository.searchTop5ByEmbeddingNear(
                        queryVector.get(),
                        MAX_COSINE_DISTANCE,
                        ScoringFunction.COSINE);
        return results.results().stream()
                .map(PetCareSearchResult::from)
                .toList();
    }
}
