package io.micronaut.samples.petclinic.service;

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

    private volatile NativeCompatibleAllMiniLmL6V2EmbeddingModel embeddingModel;

    @Override
    public FloatVector embed(String text) {
        if (text == null || text.isBlank()) {
            return new FloatVector(new float[PetCareEmbeddingDimensions.VALUE]);
        }

        float[] vector = embeddingModel().embed(text).content().vector();
        if (vector.length != PetCareEmbeddingDimensions.VALUE) {
            throw new IllegalStateException("Expected a " + PetCareEmbeddingDimensions.VALUE
                    + "-dimensional embedding but received " + vector.length);
        }
        return new FloatVector(vector);
    }

    /**
     * Creates the ONNX model only when a real embedding is requested. The model
     * loads a 90 MB resource and two native libraries, so eager construction is
     * especially problematic for GraalVM native images and for non-Oracle
     * application starts.
     */
    private NativeCompatibleAllMiniLmL6V2EmbeddingModel embeddingModel() {
        NativeCompatibleAllMiniLmL6V2EmbeddingModel model = embeddingModel;
        if (model == null) {
            synchronized (this) {
                model = embeddingModel;
                if (model == null) {
                    model = new NativeCompatibleAllMiniLmL6V2EmbeddingModel();
                    embeddingModel = model;
                }
            }
        }
        return model;
    }
}
