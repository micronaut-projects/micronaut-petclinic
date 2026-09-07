package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.samples.petclinic.dto.PetCareSearchRequest;
import io.micronaut.samples.petclinic.dto.PetCareSearchResult;
import io.micronaut.samples.petclinic.model.PetCareEmbeddingDimensions;
import io.micronaut.samples.petclinic.service.PetCareKnowledgeService;
import io.micronaut.views.View;

import java.util.List;
import java.util.Map;

/**
 * Oracle vector-search demo for retrieving pet-care document chunks.
 */
@Controller("/knowledge")
@Requires(env = "oracle")
public class PetCareKnowledgeController {

    private final PetCareKnowledgeService knowledgeService;

    /**
     * Creates the knowledge search controller.
     *
     * @param knowledgeService vector retrieval service
     */
    public PetCareKnowledgeController(PetCareKnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * Displays the semantic chunk search page.
     *
     * @param query optional natural-language query
     * @return page model
     */
    @Get
    @View("knowledge/search")
    public Map<String, Object> searchPage(@QueryValue(defaultValue = "") String query) {
        return Map.of(
                "query", query,
                "results", knowledgeService.search(query),
                "dimensions", PetCareEmbeddingDimensions.VALUE
        );
    }

    /**
     * Returns the nearest pet-care chunks as JSON.
     *
     * @param request search request
     * @return ranked chunks
     */
    @Post(value = "/search", consumes = MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<PetCareSearchResult> search(@Body PetCareSearchRequest request) {
        return knowledgeService.search(request.query());
    }
}
