package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.data.model.Sort;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.OwnerCareView;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.service.OwnerCareViewService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MicronautTest(transactional = false)
@Requires(env = "oracle")
@Property(name = "micronaut.http.client.exception-on-error-status", value = "false")
class OracleJsonDualityControllerTest {

    @Inject @Client("/") HttpClient client;
    @Inject OwnerRepository ownerRepository;
    @Inject OwnerCareViewService ownerCareViewService;

    private OwnerCareView originalDocument;

    @BeforeEach
    void loadSampleDocument() {
        Collection<Owner> matches = ownerRepository.findByLastNameContainingIgnoreCase(
                "Coleman", Sort.of(Sort.Order.asc("lastName")));
        Owner owner = matches.stream().findFirst().orElseThrow();
        originalDocument = ownerCareViewService.findById(owner.id());
    }

    @AfterEach
    void restoreSampleDocument() {
        if (originalDocument == null) {
            return;
        }
        OwnerCareView current = ownerCareViewService.findById(originalDocument.id());
        if (current != null) {
            ownerCareViewService.update(originalDocument.id(), copyWithMetadata(originalDocument, current.metadata()));
        }
    }

    @Test
    void ownerDetailsPageIncludesOracleDualityTabForThatOwner() {
        HttpResponse<String> details = client.toBlocking().exchange(
                HttpRequest.GET("/owners/" + originalDocument.id()), String.class);
        assertThat(details.body()).contains("?tab=json-duality");
        assertThat(details.body()).doesNotContain("id=\"json-duality-page\"");

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET("/owners/" + originalDocument.id() + "?tab=json-duality"), String.class);

        assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.body()).contains("Owner care document");
        assertThat(response.body()).contains("data-document-url=\"/owners/" + originalDocument.id()
                + "/json-duality\"");
    }

    @Test
    void getDocumentReturnsNestedJsonAndOracleMetadata() {
        HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET(documentUrl()).accept(MediaType.APPLICATION_JSON_TYPE), Map.class);

        assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.body()).containsEntry("_id", originalDocument.id());
        assertThat(response.body().get("pets")).isInstanceOf(java.util.List.class);
        assertThat(metadata(response.body()).get("etag")).isEqualTo(originalDocument.metadata().etag());
    }

    @Test
    void putDocumentUpdatesAndReturnsTheFreshDocument() {
        OwnerCareView update = withCity(originalDocument, originalDocument.city() + " HTTP");

        HttpResponse<Map> response = putDocument(update);

        assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.body()).containsEntry("city", update.city());
        assertThat(metadata(response.body()).get("etag")).isNotEqualTo(originalDocument.metadata().etag());
    }

    @Test
    void putDocumentReturnsBadRequestWhenNestedMembershipChanges() {
        OwnerCareView update = new OwnerCareView(originalDocument.id(), originalDocument.firstName(),
                originalDocument.lastName(), originalDocument.address(), originalDocument.city(),
                originalDocument.telephone(), java.util.List.of(), originalDocument.metadata());

        HttpResponse<Map> response = putDocument(update);

        assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
        assertThat(response.body().get("message").toString()).contains("pets");
    }

    @Test
    void putDocumentReturnsConflictForAStaleEtag() {
        HttpResponse<Map> firstUpdate = putDocument(withCity(originalDocument, originalDocument.city() + " HTTP"));
        assertThat(firstUpdate.code()).isEqualTo(HttpStatus.OK.getCode());

        HttpResponse<Map> staleUpdate = putDocument(withCity(originalDocument, originalDocument.city() + " stale"));

        assertThat(staleUpdate.code()).isEqualTo(HttpStatus.CONFLICT.getCode());
        assertThat(staleUpdate.body().get("message").toString()).contains("changed after it was loaded");
    }

    @Test
    void getDocumentReturnsNotFoundForUnknownOwner() {
        assertThatThrownBy(() -> client.toBlocking().exchange(
                HttpRequest.GET("/owners/999999/json-duality")
                        .accept(MediaType.APPLICATION_JSON_TYPE), Map.class))
                .isInstanceOfSatisfying(HttpClientResponseException.class, exception -> {
                    assertThat(exception.getStatus().getCode()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
                    assertThat(exception.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
                });
    }

    private String documentUrl() {
        return "/owners/" + originalDocument.id() + "/json-duality";
    }

    private HttpResponse<Map> putDocument(OwnerCareView document) {
        HttpRequest<OwnerCareView> request = HttpRequest.PUT(documentUrl(), document)
                .contentType(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE);
        return client.toBlocking().exchange(request, Argument.of(Map.class), Argument.of(Map.class));
    }

    private static Map<?, ?> metadata(Map<?, ?> document) {
        return (Map<?, ?>) document.get("_metadata");
    }

    private static OwnerCareView withCity(OwnerCareView source, String city) {
        return new OwnerCareView(source.id(), source.firstName(), source.lastName(), source.address(), city,
                source.telephone(), source.pets(), source.metadata());
    }

    private static OwnerCareView copyWithMetadata(OwnerCareView source, OwnerCareView.DualityMetadata metadata) {
        return new OwnerCareView(source.id(), source.firstName(), source.lastName(), source.address(), source.city(),
                source.telephone(), source.pets(), metadata);
    }
}
