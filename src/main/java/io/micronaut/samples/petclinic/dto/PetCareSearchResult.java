package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.samples.petclinic.model.PetCareChunk;

/**
 * JSON-friendly representation of a ranked vector search result.
 *
 * @param id chunk identifier
 * @param documentTitle source document title
 * @param documentSource source label
 * @param topic knowledge topic
 * @param species primary species
 * @param chunkIndex position within the document
 * @param content matching chunk text
 * @param distance vector distance; lower is a closer match
 * @param similarity optional similarity value returned by the driver
 */
@Serdeable
public record PetCareSearchResult(
        Integer id,
        String documentTitle,
        String documentSource,
        String topic,
        String species,
        Integer chunkIndex,
        String content,
        double distance,
        Double similarity
) {

    /**
     * Maps a Micronaut Data vector search result to an API result.
     *
     * @param result the framework search result
     * @return a serializable result
     */
    public static PetCareSearchResult from(
            io.micronaut.data.model.vector.search.SearchResult<PetCareChunk> result) {
        PetCareChunk chunk = result.entity();
        Double similarity = result.similarity() == null ? null : result.similarity().value();
        return new PetCareSearchResult(
                chunk.id(),
                chunk.documentTitle(),
                chunk.documentSource(),
                chunk.topic(),
                chunk.species(),
                chunk.chunkIndex(),
                chunk.content(),
                result.score().value(),
                similarity
        );
    }
}
