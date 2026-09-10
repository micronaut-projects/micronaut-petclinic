package io.micronaut.samples.petclinic.model;

/**
 * Shared dimension for the precomputed pet-care vectors and Oracle VECTOR column.
 */
public final class PetCareEmbeddingDimensions {

    /** Number of dimensions stored in Oracle. */
    public static final int VALUE = 384;

    private PetCareEmbeddingDimensions() {
    }
}
