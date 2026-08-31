package io.micronaut.samples.petclinic.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.micronaut.context.annotation.Requires;
import io.micronaut.data.model.vector.FloatVector;
import io.micronaut.samples.petclinic.model.PetCareEmbeddingDimensions;
import jakarta.inject.Singleton;

/**
 * Local sentence embedding service backed by LangChain4j and
 * all-MiniLM-L6-v2 running in-process through ONNX Runtime.
 * <p>
 * This is an embedding model only; it does not generate answers or require an
 * LLM provider. The model produces 384-dimensional vectors, matching the
 * Oracle {@code VECTOR(384, FLOAT32)} column.
 */
@Singleton
@Requires(env = "oracle")
public class LangChain4jPetCareEmbeddingService implements PetCareEmbeddingService {

    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    @Override
    public FloatVector embed(String text) {
        if (text == null || text.isBlank()) {
            return new FloatVector(new float[PetCareEmbeddingDimensions.VALUE]);
        }

        Response<Embedding> response = embeddingModel.embed(text);
        float[] vector = response.content().vector();
        if (vector.length != PetCareEmbeddingDimensions.VALUE) {
            throw new IllegalStateException("Expected a " + PetCareEmbeddingDimensions.VALUE
                    + "-dimensional embedding but received " + vector.length);
        }
        return new FloatVector(vector);
    }
}
