package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.model.vector.search.Score;
import io.micronaut.data.model.vector.search.ScoringFunction;
import io.micronaut.data.model.vector.search.SearchResults;
import io.micronaut.samples.petclinic.model.PetCareChunk;
import io.micronaut.samples.petclinic.service.PetCareEmbeddingService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Oracle integration tests for Micronaut Data vector repository support.
 * <p>
 * These tests are enabled explicitly with {@code MICRONAUT_ENVIRONMENTS=oracle}
 * because they require the Oracle container and VECTOR column support.
 */
@MicronautTest(environments = "oracle")
@EnabledIfEnvironmentVariable(named = "MICRONAUT_ENVIRONMENTS", matches = ".*oracle.*")
class OraclePetCareChunkRepositoryTest {

    @Inject
    PetCareChunkRepository chunkRepository;

    @Inject
    PetCareEmbeddingService embeddingService;

    @Test
    void searchesSeededChunksInDistanceOrder() {
        SearchResults<PetCareChunk> results = chunkRepository.searchTop5ByEmbeddingNear(
                embeddingService.embed("What vaccinations does my puppy need?"),
                new Score(2.0),
                ScoringFunction.COSINE
        );

        assertThat(results.results()).hasSize(2);
        assertThat(results.results().getFirst().entity().documentTitle()).isEqualTo("Dog preventive care");
        assertThat(results.results().getFirst().score().value())
                .isLessThanOrEqualTo(results.results().get(1).score().value());
    }

    @Test
    void returnsEmptySearchResultsWhenDistanceThresholdExcludesEveryChunk() {
        SearchResults<PetCareChunk> results = chunkRepository.searchTop5ByEmbeddingNear(
                embeddingService.embed("something unrelated to pet care"),
                new Score(-1.0),
                ScoringFunction.COSINE
        );

        assertThat(results.results()).isEmpty();
    }
}
