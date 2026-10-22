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
import io.micronaut.http.client.annotation.Client;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.oracle.model.OwnerCareView;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.service.OwnerCareViewService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest(transactional = false)
@Requires(env = "oracle")
@Property(name = "micronaut.http.client.exception-on-error-status", value = "false")
class OwnerCareRecordControllerTest {

    @Inject @Client("/") HttpClient client;
    @Inject OwnerRepository ownerRepository;
    @Inject OwnerCareViewService ownerCareViewService;

    private OwnerCareView originalDocument;

    @BeforeEach
    void loadSampleDocument() throws NotFoundEntityException {
        Collection<Owner> matches = ownerRepository.findByLastNameContainsIgnoreCase(
                "Coleman", Sort.of(Sort.Order.asc("lastName")));
        Owner owner = matches.stream().findFirst().orElseThrow();
        originalDocument = ownerCareViewService.findById(owner.id());
    }

    @AfterEach
    void restoreSampleDocument() throws NotFoundEntityException {
        if (originalDocument == null) {
            return;
        }
        OwnerCareView current = ownerCareViewService.findById(originalDocument.id());
        ownerCareViewService.update(copyWithMetadata(originalDocument, current.metadata()));
    }

    @Test
    void ownerDetailsPageIncludesCareRecordTabForThatOwner() {
        HttpResponse<String> details = client.toBlocking().exchange(
                HttpRequest.GET("/owners/" + originalDocument.id()), String.class);
        assertThat(details.body()).contains("id=\"care-record-tab\"");
        assertThat(details.body()).contains("id=\"care-record-panel\" hidden");
        assertThat(details.body()).doesNotContain("?tab=care-record");
        assertThat(details.body()).contains("data-document-url=\"/owners/" + originalDocument.id()
                + "/care-record\"");

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET("/owners/" + originalDocument.id() + "?tab=care-record"), String.class);

        assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.body()).contains("Owner care document");
        assertThat(response.body()).contains("id=\"care-record-tab\"");
        assertThat(response.body()).contains("data-document-url=\"/owners/" + originalDocument.id()
                + "/care-record\"");
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
    void putDocumentUpdatesAndReturnsTheDocument() {
        OwnerCareView update = withCity(originalDocument, originalDocument.city() + " HTTP");

        HttpResponse<Map> response = putDocument(update);

        assertThat(response.code()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.body()).containsEntry("city", update.city());
    }

    @Test
    void putDocumentRejectsNestedMembershipChanges() {
        OwnerCareView update = new OwnerCareView(originalDocument.id(), originalDocument.firstName(),
                originalDocument.lastName(), originalDocument.address(), originalDocument.city(),
                originalDocument.telephone(), java.util.List.of(), originalDocument.metadata());

        HttpResponse<Map> response = putDocument(update);

        assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
    }

    @Test
    void putDocumentRejectsAStaleEtag() {
        HttpResponse<Map> firstUpdate = putDocument(withCity(originalDocument, originalDocument.city() + " HTTP"));
        assertThat(firstUpdate.code()).isEqualTo(HttpStatus.OK.getCode());

        HttpResponse<Map> staleUpdate = putDocument(withCity(originalDocument, originalDocument.city() + " stale"));

        assertThat(staleUpdate.code()).isEqualTo(HttpStatus.CONFLICT.getCode());
        assertThat(staleUpdate.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(staleUpdate.body()).containsEntry(
                "message", "This owner record changed after it was loaded. Reload it before saving.");
    }

    @Test
    void getDocumentReturnsNotFoundForUnknownOwner() {
        HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.GET("/owners/999999999/care-record")
                        .accept(MediaType.APPLICATION_JSON_TYPE), Argument.of(Map.class), Argument.of(Map.class));
        assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.body()).containsEntry("message", "Entity not found.");
    }

    @Test
    void putDocumentReturnsNotFoundForUnknownOwner() {
        OwnerCareView missing = new OwnerCareView(999999999, originalDocument.firstName(),
                originalDocument.lastName(), originalDocument.address(), originalDocument.city(),
                originalDocument.telephone(), originalDocument.pets(), originalDocument.metadata());

        HttpResponse<Map> response = client.toBlocking().exchange(
                HttpRequest.PUT("/owners/999999999/care-record", missing)
                        .contentType(MediaType.APPLICATION_JSON_TYPE)
                        .accept(MediaType.APPLICATION_JSON_TYPE),
                Argument.of(Map.class), Argument.of(Map.class));

        assertThat(response.code()).isEqualTo(HttpStatus.NOT_FOUND.getCode());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.body()).containsEntry("message", "Entity not found.");
    }

    @Test
    void secondSaveUsingReturnedMetadataIsOK() {
        HttpResponse<OwnerCareView> first = client.toBlocking().exchange(
                HttpRequest.PUT(documentUrl(), withCity(originalDocument, "First save")), OwnerCareView.class);
        HttpResponse<Map> second = putDocument(withCity(Objects.requireNonNull(first.body()), "Second save"));
        assertThat(second.code()).isEqualTo(HttpStatus.OK.getCode());
    }

    @Test
    void rejectsMismatchedDocumentId() throws NotFoundEntityException {
        assertRejectedDocument(new OwnerCareView(999999999, originalDocument.firstName(),
                originalDocument.lastName(), originalDocument.address(), originalDocument.city(),
                originalDocument.telephone(), originalDocument.pets(), originalDocument.metadata()));
    }

    @Test
    void rejectsMissingMetadataAndBlankEtag() throws NotFoundEntityException {
        assertRejectedDocument(copyWithMetadata(originalDocument, null));
        assertRejectedDocument(copyWithMetadata(originalDocument, new OwnerCareView.DualityMetadata("", null)));
    }

    @Test
    void rejectsInvalidNestedFields() throws NotFoundEntityException {
        var pet = originalDocument.pets().getFirst();
        var visit = pet.visits().getFirst();
        for (var invalidPet : java.util.List.of(
                new OwnerCareView.PetCareSubView(pet.id(), "", pet.birthDate(), pet.visits()),
                new OwnerCareView.PetCareSubView(pet.id(), pet.name(), null, pet.visits()),
                new OwnerCareView.PetCareSubView(pet.id(), pet.name(), pet.birthDate(),
                        java.util.List.of(new OwnerCareView.VisitCareSubView(visit.id(), null, ""))))) {
            assertRejectedDocument(new OwnerCareView(originalDocument.id(), originalDocument.firstName(),
                    originalDocument.lastName(), originalDocument.address(), originalDocument.city(),
                    originalDocument.telephone(), originalDocument.pets().stream()
                            .map(existing -> existing.id().equals(pet.id()) ? invalidPet : existing).toList(),
                    originalDocument.metadata()));
        }
    }

    private void assertRejectedDocument(OwnerCareView document) throws NotFoundEntityException {
        HttpResponse<Map> response = putDocument(document);
        assertThat(response.code()).isEqualTo(HttpStatus.BAD_REQUEST.getCode());
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        OwnerCareView current = ownerCareViewService.findById(originalDocument.id());
        assertThat(current.city()).isEqualTo(originalDocument.city());
        assertThat(current.pets()).isEqualTo(originalDocument.pets());
        assertThat(current.metadata().etag()).isEqualTo(originalDocument.metadata().etag());
    }

    private String documentUrl() {
        return "/owners/" + originalDocument.id() + "/care-record";
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
