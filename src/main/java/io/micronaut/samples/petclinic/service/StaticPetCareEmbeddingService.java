package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.model.vector.FloatVector;
import io.micronaut.samples.petclinic.model.PetCareEmbeddingDimensions;
import jakarta.inject.Singleton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Supplies vectors captured from the all-MiniLM-L6-v2 model during the JVM
 * data-load run. No embedding model or native library is loaded at runtime.
 */
@Singleton
@Requires(env = "oracle")
public final class StaticPetCareEmbeddingService implements PetCareEmbeddingService {

    private static final String VECTOR_RESOURCE = "/knowledge/pet-care-embeddings.tsv";
    private final Map<String, FloatVector> vectors;

    public StaticPetCareEmbeddingService() {
        vectors = loadVectors();
    }

    @Override
    public FloatVector embed(String text) {
        if (text == null || text.isBlank()) {
            return new FloatVector(new float[PetCareEmbeddingDimensions.VALUE]);
        }

        FloatVector vector = vectors.get(text.trim());
        if (vector == null) {
            throw new IllegalArgumentException("No precomputed pet-care vector for query: " + text);
        }
        return copy(vector);
    }

    @Override
    public Optional<FloatVector> find(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        FloatVector vector = vectors.get(text.trim());
        return vector == null ? Optional.empty() : Optional.of(copy(vector));
    }

    private static Map<String, FloatVector> loadVectors() {
        Map<String, FloatVector> loaded = new HashMap<>();
        try (InputStream input = StaticPetCareEmbeddingService.class.getResourceAsStream(VECTOR_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing vector resource: " + VECTOR_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] fields = line.split("\\t", -1);
                    if (fields.length != 3 || (!fields[0].equals("C") && !fields[0].equals("Q"))) {
                        throw new IllegalStateException("Invalid vector resource line " + lineNumber);
                    }
                    String text = new String(Base64.getDecoder().decode(fields[1]), StandardCharsets.UTF_8);
                    loaded.put(text, decodeVector(fields[2], lineNumber));
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Could not load precomputed pet-care vectors", e);
        }
        if (loaded.isEmpty()) {
            throw new IllegalStateException("No precomputed pet-care vectors found");
        }
        return Map.copyOf(loaded);
    }

    private static FloatVector decodeVector(String encoded, int lineNumber) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        int expectedBytes = PetCareEmbeddingDimensions.VALUE * Float.BYTES;
        if (bytes.length != expectedBytes) {
            throw new IllegalStateException("Vector resource line " + lineNumber
                    + " contains " + bytes.length + " bytes; expected " + expectedBytes);
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] values = new float[PetCareEmbeddingDimensions.VALUE];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.getFloat();
        }
        return new FloatVector(values);
    }

    private static FloatVector copy(FloatVector vector) {
        return new FloatVector(vector.data().clone());
    }
}
