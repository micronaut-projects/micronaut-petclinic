package io.micronaut.samples.petclinic.service;

import io.micronaut.data.model.vector.FloatVector;
import io.micronaut.samples.petclinic.model.PetCareEmbeddingDimensions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the local LangChain4j sentence embedding used by the Oracle showcase.
 */
class LangChain4jPetCareEmbeddingServiceTest {

    private final LangChain4jPetCareEmbeddingService embeddingService =
            new LangChain4jPetCareEmbeddingService();

    @Test
    void createsTheConfiguredVectorSize() {
        FloatVector vector = embeddingService.embed("What vaccinations does my puppy need?");

        assertThat(vector.data()).hasSize(PetCareEmbeddingDimensions.VALUE);
    }

    @Test
    void returnsAZeroVectorForBlankText() {
        FloatVector vector = embeddingService.embed("   ");

        assertThat(vector.data()).hasSize(PetCareEmbeddingDimensions.VALUE);
        for (float value : vector.data()) {
            assertThat(value).isEqualTo(0.0f);
        }
    }

    @Test
    void givesRelatedPetCareQuestionsSimilarVectors() {
        FloatVector puppyVector = embeddingService.embed("What vaccinations does my puppy need?");
        FloatVector dogVector = embeddingService.embed("When should a young dog receive its shots?");

        assertThat(cosine(puppyVector.data(), dogVector.data())).isGreaterThan(0.50);
    }

    @Test
    void givesDifferentPetCareTopicsLessSimilarVectors() {
        FloatVector birdVector = embeddingService.embed("How should I clean my bird's cage?");
        FloatVector reptileVector = embeddingService.embed("What temperature and humidity does a reptile habitat need?");

        assertThat(cosine(birdVector.data(), reptileVector.data())).isLessThan(0.80);
    }

    private static double cosine(float[] left, float[] right) {
        double dot = 0.0;
        double leftLength = 0.0;
        double rightLength = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftLength += left[i] * left[i];
            rightLength += right[i] * right[i];
        }
        return dot / Math.sqrt(leftLength * rightLength);
    }
}
