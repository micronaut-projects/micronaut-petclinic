package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.micronaut.samples.petclinic.model.PetCareChunk;
import io.micronaut.samples.petclinic.model.PetCareDocument;
import io.micronaut.samples.petclinic.repository.PetCareChunkRepository;
import io.micronaut.samples.petclinic.repository.PetCareDocumentRepository;
import io.micronaut.samples.petclinic.service.PetCareEmbeddingService;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.io.InputStream;

/**
 * Seeds a small, repeatable chunked knowledge base for the Oracle showcase.
 */
@Singleton
@Requires(env = "oracle")
public class PetCareKnowledgeLoader implements ApplicationEventListener<StartupEvent> {

    private static final String KNOWLEDGE_RESOURCE = "/knowledge/pet-care-knowledge.json";

    private final PetCareDocumentRepository documentRepository;
    private final PetCareChunkRepository chunkRepository;
    private final PetCareEmbeddingService embeddingService;
    private final JsonMapper jsonMapper;

    /**
     * Creates the knowledge-base loader.
     *
     * @param documentRepository document repository
     * @param chunkRepository chunk repository
     * @param embeddingService embedding service
     * @param jsonMapper mapper used to read the knowledge resource
     */
    public PetCareKnowledgeLoader(PetCareDocumentRepository documentRepository,
                                  PetCareChunkRepository chunkRepository,
                                  PetCareEmbeddingService embeddingService,
                                  JsonMapper jsonMapper) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public void onApplicationEvent(StartupEvent event) {
        if (documentRepository.count() > 0 || chunkRepository.count() > 0) {
            return;
        }

        try (InputStream input = PetCareKnowledgeLoader.class.getResourceAsStream(KNOWLEDGE_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing knowledge resource: " + KNOWLEDGE_RESOURCE);
            }
            JsonNode root = jsonMapper.readValue(input, JsonNode.class);
            JsonNode documents = required(root, "documents");
            for (JsonNode document : documents.values()) {
                seedDocument(
                        required(document, "title").coerceStringValue(),
                        required(document, "source").coerceStringValue(),
                        required(document, "description").coerceStringValue(),
                        required(document, "chunks")
                );
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read knowledge resource: " + KNOWLEDGE_RESOURCE, e);
        }
    }

    private void seedDocument(String title,
                              String source,
                              String description,
                              JsonNode chunks) {
        PetCareDocument document = documentRepository.save(new PetCareDocument(title, source, description));
        int index = 0;
        for (JsonNode chunk : chunks.values()) {
            String content = required(chunk, "content").coerceStringValue();
            chunkRepository.save(new PetCareChunk(
                    document.id(),
                    document.title(),
                    document.source(),
                    required(chunk, "topic").coerceStringValue(),
                    required(chunk, "species").coerceStringValue(),
                    index,
                    content,
                    embeddingService.embed(content)
            ));
            index++;
        }
    }

    private static JsonNode required(JsonNode parent, String field) {
        JsonNode value = parent.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalStateException("Missing field '" + field + "' in " + KNOWLEDGE_RESOURCE);
        }
        return value;
    }
}
