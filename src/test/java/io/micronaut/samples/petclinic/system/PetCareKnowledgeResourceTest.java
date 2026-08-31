package io.micronaut.samples.petclinic.system;

import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the external knowledge seed resource has the shape expected by the loader.
 */
class PetCareKnowledgeResourceTest {

    private static final String RESOURCE = "/knowledge/pet-care-knowledge.json";

    @Test
    void containsMultipleDocumentsAndSearchableChunks() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(RESOURCE)) {
            assertThat(input).isNotNull();

            JsonNode root = JsonMapper.createDefault().readValue(input, JsonNode.class);
            JsonNode documents = root.get("documents");

            assertThat(documents).isNotNull();
            assertThat(documents.size()).isGreaterThanOrEqualTo(10);

            int chunkCount = 0;
            for (JsonNode document : documents.values()) {
                assertThat(document.get("title").coerceStringValue()).isNotBlank();
                assertThat(document.get("source").coerceStringValue()).isNotBlank();
                JsonNode chunks = document.get("chunks");
                assertThat(chunks).isNotNull();
                assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
                chunkCount += chunks.size();
                for (JsonNode chunk : chunks.values()) {
                    assertThat(chunk.get("topic").coerceStringValue()).isNotBlank();
                    assertThat(chunk.get("species").coerceStringValue()).isNotBlank();
                    assertThat(chunk.get("content").coerceStringValue()).isNotBlank();
                }
            }

            assertThat(chunkCount).isGreaterThanOrEqualTo(20);
        }
    }
}
