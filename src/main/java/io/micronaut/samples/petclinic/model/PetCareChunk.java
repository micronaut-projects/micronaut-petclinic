package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.VectorStorage;
import io.micronaut.data.model.vector.FloatVector;
import io.micronaut.serde.annotation.Serdeable;

/**
 * A searchable chunk of a pet-care document.
 *
 * @param id the database identifier
 * @param documentId the owning document identifier
 * @param documentTitle denormalized title used by the search result view
 * @param documentSource denormalized source used by the search result view
 * @param topic the knowledge topic
 * @param species the primary species covered by the chunk
 * @param chunkIndex the zero-based position within the document
 * @param content the chunk text
 * @param embedding the 384-dimensional vector used for semantic search
 */
@MappedEntity("PET_CARE_CHUNKS")
@Serdeable
public record PetCareChunk(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("DOCUMENT_ID")
        Integer documentId,

        @MappedProperty("DOCUMENT_TITLE")
        String documentTitle,

        @MappedProperty("DOCUMENT_SOURCE")
        String documentSource,

        @MappedProperty("TOPIC")
        String topic,

        @MappedProperty("SPECIES")
        String species,

        @MappedProperty("CHUNK_INDEX")
        Integer chunkIndex,

        @MappedProperty("CONTENT")
        String content,

        @MappedProperty("EMBEDDING")
        @VectorStorage(length = PetCareEmbeddingDimensions.VALUE)
        FloatVector embedding
) {

    /**
     * Creates a chunk that has not yet been persisted.
     *
     * @param documentId the owning document identifier
     * @param documentTitle the document title
     * @param documentSource the document source label
     * @param topic the knowledge topic
     * @param species the primary species covered by the chunk
     * @param chunkIndex the zero-based position within the document
     * @param content the chunk text
     * @param embedding the vector representation of the chunk
     */
    public PetCareChunk(Integer documentId,
                        String documentTitle,
                        String documentSource,
                        String topic,
                        String species,
                        Integer chunkIndex,
                        String content,
                        FloatVector embedding) {
        this(null, documentId, documentTitle, documentSource, topic, species, chunkIndex, content, embedding);
    }
}
