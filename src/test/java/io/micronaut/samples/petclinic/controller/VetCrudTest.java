package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.samples.petclinic.dto.SpecialityDto;
import io.micronaut.samples.petclinic.dto.VetDto;
import io.micronaut.samples.petclinic.dto.VetForm;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.repository.SpecialityRepository;
import io.micronaut.samples.petclinic.repository.VetRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(transactional = false)
@Property(name = "micronaut.http.client.follow-redirects", value = "false")
class VetCrudTest {
    @Inject
    @Client("/")
    HttpClient client;
    @Inject
    VetRepository vets;
    @Inject
    SpecialityRepository specialities;

    private final List<Integer> vetIds = new ArrayList<>();
    private final List<Integer> specialityIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        vetIds.forEach(id -> {
            vets.deleteSpecialityLinks(id);
            vets.findById(id).ifPresent(vets::delete);
        });
        specialityIds.forEach(specialities::deleteById);
    }

    @Test
    void createsReadsUpdatesAndDeletesUsingEtags() {
        SpecialityDto speciality = createSpeciality();
        HttpResponse<VetDto> created = createVet(Set.of(speciality));
        VetDto vet = created.body();
        String url = "/vets/" + Objects.requireNonNull(vet).id();
        String etag = created.header("ETag");
        Objects.requireNonNull(etag);
        assertThat((Object) created.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(etag).isEqualTo("\"" + vet.etag() + "\"");
        assertThat(created.header("Cache-Control")).isEqualTo("no-store");

        HttpResponse<VetDto> loaded = client.toBlocking().exchange(HttpRequest.GET(url), VetDto.class);
        assertThat(Objects.requireNonNull(loaded.body()).specialities()).containsExactly(speciality);
        assertThat(loaded.header("ETag")).isEqualTo(etag);

        HttpResponse<VetDto> updated = client.toBlocking().exchange(HttpRequest.PUT(url,
                new VetForm("Updated", vet.lastName(), Set.of())).header("If-Match", etag), VetDto.class);
        assertThat(Objects.requireNonNull(updated.body()).firstName()).isEqualTo("Updated");
        assertThat(updated.header("ETag")).isNotEqualTo(etag);
        assertThat(vets.findById(vet.id()).orElseThrow().specialities()).isEmpty();
        assertThat(specialities.findById(speciality.id())).isPresent();

        HttpResponse<?> deleted = client.toBlocking().exchange(HttpRequest.DELETE(url)
                .header("If-Match", Objects.requireNonNull(updated.header("ETag"))));
        assertThat((Object) deleted.status()).isEqualTo(HttpStatus.NO_CONTENT);
        assertError(HttpRequest.GET(url), HttpStatus.NOT_FOUND);
    }

    @Test
    void createsVetUsingMultipleSpecialityIdsWithoutNames() {
        SpecialityDto first = createSpeciality();
        SpecialityDto second = createSpeciality();
        HttpResponse<VetDto> created = createVet(Set.of(
                new SpecialityDto(first.id(), null), new SpecialityDto(second.id(), null)));

        assertThat(created.code()).isEqualTo(201);
        assertThat(created.body().specialities()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void rejectsStaleUpdatesAndDeletesWithoutChangingNamesOrLinks() {
        SpecialityDto first = createSpeciality();
        SpecialityDto second = createSpeciality();
        HttpResponse<VetDto> created = createVet(Set.of(first));
        String etagHeader = created.header("Etag");
        Objects.requireNonNull(etagHeader);
        VetDto vet = created.body();
        String url = "/vets/" + Objects.requireNonNull(vet).id();
        HttpResponse<VetDto> updated = client.toBlocking().exchange(HttpRequest.PUT(url,
                        new VetForm("Winner", vet.lastName(), Set.of(second.id())))
                .header("If-Match", etagHeader), VetDto.class);
        assertThat(Objects.requireNonNull(updated.body()).specialities()).containsExactly(second);

        assertError(HttpRequest.PUT(url, new VetForm("Loser", "Changed", Set.of(first.id())))
                .header("If-Match", etagHeader), HttpStatus.PRECONDITION_FAILED);
        assertError(HttpRequest.DELETE(url).header("If-Match", etagHeader), HttpStatus.PRECONDITION_FAILED);
        VetDto current = client.toBlocking().retrieve(url, VetDto.class);
        assertThat(current).isEqualTo(updated.body());
    }

    @Test
    void requiresAnEtagForJsonMutations() {
        VetDto vet = createVet(Set.of()).body();
        String url = "/vets/" + vet.id();
        assertError(HttpRequest.PUT(url, new VetForm(vet.firstName(), vet.lastName(), new HashSet<>(vetIds))), HttpStatus.PRECONDITION_REQUIRED);
        assertError(HttpRequest.DELETE(url), HttpStatus.PRECONDITION_REQUIRED);
        assertJsonError(HttpRequest.PUT(url, new VetForm("Other", "Name", Set.of())), HttpStatus.PRECONDITION_REQUIRED);
        assertJsonError(HttpRequest.DELETE(url).header("If-Match", ""), HttpStatus.PRECONDITION_REQUIRED);
        assertThat(vets.findById(vet.id())).isPresent();
    }

    @Test
    void rejectsUnknownSpecialitiesWithAnActionableBadRequest() {
        HttpClientResponseException error = assertError(HttpRequest.POST("/vets",
                        new VetForm("Unknown", "Speciality", Set.of(Integer.MAX_VALUE))),
                HttpStatus.BAD_REQUEST);
        assertThat(error.getResponse().getBody(String.class).orElseThrow()).contains("specialities");
    }

    @Test
    void rejectsBlankNames() {
        assertError(HttpRequest.POST("/vets", new VetForm(" ", "Name", Set.of())), HttpStatus.BAD_REQUEST);
        assertError(HttpRequest.POST("/vets/specialities", new SpecialityDto(null, " ")), HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsSpecialitiesWithoutAnIdentifier() {
        assertError(HttpRequest.POST("/vets", new VetForm("Review", "MissingId",
                Collections.singleton(null))), HttpStatus.BAD_REQUEST);
    }

    @Test
    void listsVetsInLastNameOrder() {
        VetDto[] listed = client.toBlocking().retrieve("/vets/json", VetDto[].class);
        assertThat(listed).extracting(VetDto::lastName).isSorted();
    }

    @Test
    void deletingAVetRemovesLinksButPreservesSharedSpecialities() {
        SpecialityDto speciality = createSpeciality();
        HttpResponse<VetDto> first = createVet(Set.of(speciality));
        VetDto second = createVet(Set.of(speciality)).body();
        client.toBlocking().exchange(HttpRequest.DELETE("/vets/" + first.body().id())
                .header("If-Match", Objects.requireNonNull(first.header("Etag"))));
        assertThat(vets.findById(first.body().id())).isEmpty();
        assertThat(vets.countSpecialityLinks(first.body().id())).isZero();
        assertThat(vets.countSpecialityLinks(second.id())).isEqualTo(1);
        assertThat(client.toBlocking().retrieve("/vets/" + second.id(), VetDto.class).specialities()).containsExactly(speciality);
    }

    @Test
    void staleFormUpdatesRollBackSpecialityLinkChanges() {
        SpecialityDto speciality = createSpeciality();
        HttpResponse<VetDto> created = createVet(Set.of(speciality));
        VetDto vet = created.body();
        String url = "/vets/" + vet.id();
        client.toBlocking().exchange(HttpRequest.PUT(url,
                        new VetForm("Winner", vet.lastName(), Set.of(speciality.id())))
                .header("If-Match", Objects.requireNonNull(created.header("ETag"))), VetDto.class);
        assertJsonError(HttpRequest.PUT(url,
                new VetForm("Loser", "Changed", Set.of()))
                .header("If-Match", Objects.requireNonNull(created.header("ETag"))), HttpStatus.PRECONDITION_FAILED);
        assertJsonError(HttpRequest.DELETE(url).header("If-Match", Objects.requireNonNull(created.header("ETag"))), HttpStatus.PRECONDITION_FAILED);
        VetDto current = client.toBlocking().retrieve(url, VetDto.class);
        assertThat(current.firstName()).isEqualTo("Winner");
        assertThat(current.specialities()).containsExactly(speciality);
        assertThat(vets.countSpecialityLinks(vet.id())).isEqualTo(1);
    }

    @Test
    void specialityOnlyUpdateChangesEtagAndRejectsAnotherUpdateWithTheOldEtag() {
        SpecialityDto originalSpeciality = createSpeciality();
        SpecialityDto changedSpeciality = createSpeciality();
        HttpResponse<VetDto> created = createVet(Set.of(originalSpeciality));
        VetDto vet = created.body();
        String url = "/vets/" + vet.id();
        String originalEtag = created.header("ETag");

        HttpResponse<VetDto> firstUpdate = client.toBlocking().exchange(HttpRequest.PUT(url,
                        new VetForm(vet.firstName(), vet.lastName(), Set.of(changedSpeciality.id())))
                .header("If-Match", originalEtag), VetDto.class);

        assertThat(firstUpdate.code()).isEqualTo(200);
        assertThat(firstUpdate.header("ETag")).isNotEqualTo(originalEtag);
        assertError(HttpRequest.PUT(url,
                        new VetForm(vet.firstName(), vet.lastName(), Set.of(originalSpeciality.id())))
                .header("If-Match", originalEtag), HttpStatus.PRECONDITION_FAILED);
        assertThat(client.toBlocking().retrieve(url, VetDto.class).specialities())
                .containsExactly(changedSpeciality);
    }

    @Test
    void specialityHashIsNotExposedByTheVetJsonList() {
        createVet(Set.of(createSpeciality()));

        String vetsJson = client.toBlocking().retrieve("/vets/json");

        assertThat(vetsJson).doesNotContain("specialitiesHash", "SPECIALITIES_ETAG_HASH");
    }

    @Test
    void specialityCanKeepItsNameAndRenameWithoutLosingAssignments() {
        SpecialityDto speciality = createSpeciality();
        VetDto vet = createVet(Set.of(speciality)).body();
        String url = "/vets/specialities/" + speciality.id();
        SpecialityDto unchanged = client.toBlocking().retrieve(HttpRequest.PUT(url, speciality), SpecialityDto.class);
        assertThat(unchanged).isEqualTo(speciality);
        SpecialityDto renamed = client.toBlocking().retrieve(HttpRequest.PUT(url,
                new SpecialityDto(speciality.id(), speciality.name() + " renamed")), SpecialityDto.class);
        assertThat(client.toBlocking().retrieve("/vets/" + vet.id(), VetDto.class).specialities()).containsExactly(renamed);
    }

    @Test
    void updatingAVetDoesNotOverwriteAConcurrentSpecialityRename() {
        SpecialityDto speciality = createSpeciality();
        VetDto vet = createVet(Set.of(speciality)).body();
        Vet loadedVet = vets.findById(vet.id()).orElseThrow();

        Speciality renamed = specialities.update(specialities.findById(speciality.id()).orElseThrow()
                .withName(speciality.name() + " renamed"));
        vets.update(loadedVet.withFirstName("Updated"));

        assertThat(specialities.findById(speciality.id()).orElseThrow().name()).isEqualTo(renamed.name());
        assertThat(vets.findById(vet.id()).orElseThrow().specialities())
                .extracting(Speciality::name).containsExactly(renamed.name());
    }

    @Test
    void rejectsDuplicateSpecialityCreatesAndRenames() {
        SpecialityDto first = createSpeciality();
        SpecialityDto second = createSpeciality();
        HttpClientResponseException error = assertError(HttpRequest.POST("/vets/specialities", first), HttpStatus.BAD_REQUEST);
        assertThat(error.getResponse().getBody(String.class).orElseThrow()).contains("already exists");
        assertError(HttpRequest.PUT("/vets/specialities/" + second.id(), first), HttpStatus.BAD_REQUEST);
        assertThat(specialities.findById(second.id()).orElseThrow().name()).isEqualTo(second.name());
    }

    @Test
    void specialityFormsCreateAndUpdateUsingJson() {
        String name = "form-" + UUID.randomUUID();
        HttpResponse<SpecialityDto> created = client.toBlocking().exchange(
                HttpRequest.POST("/vets/specialities", Map.of("name", name)), SpecialityDto.class);
        SpecialityDto speciality = created.body();
        specialityIds.add(speciality.id());
        assertThat(created.code()).isEqualTo(201);
        assertThat(created.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(created.header("Location")).isNull();
        assertThat(speciality.name()).isEqualTo(name);
        assertThat(client.toBlocking().retrieve("/vets/specialities?created=true"))
                .contains("Speciality created.", name);
        VetDto vet = createVet(Set.of(speciality)).body();

        String editUrl = "/vets/specialities/" + speciality.id();
        HttpResponse<SpecialityDto> unchanged = client.toBlocking().exchange(
                HttpRequest.PUT(editUrl, speciality), SpecialityDto.class);
        assertThat(unchanged.body()).isEqualTo(speciality);
        HttpResponse<SpecialityDto> updated = client.toBlocking().exchange(
                HttpRequest.PUT(editUrl, Map.of("name", name + " renamed")), SpecialityDto.class);
        assertThat(updated.code()).isEqualTo(200);
        assertThat(updated.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(updated.header("Location")).isNull();
        assertThat(updated.body()).isEqualTo(new SpecialityDto(speciality.id(), name + " renamed"));
        assertThat(client.toBlocking().retrieve("/vets/" + vet.id(), VetDto.class).specialities())
                .containsExactly(updated.body());
        assertThat(client.toBlocking().retrieve("/vets/specialities?changed=true"))
                .contains("Speciality changed.", name + " renamed");
        assertThat(client.toBlocking().retrieve("/vets/specialities/new"))
                .contains("id=\"speciality-form\"", "action=\"/vets/specialities\"", "/resources/js/speciality-form.js");
        assertThat(client.toBlocking().retrieve("/vets/specialities/" + speciality.id() + "/edit?saved=true"))
                .contains("id=\"speciality-saved-alert\"", "id=\"speciality-feedback\"", "Speciality saved.", name + " renamed");
        assertThat(client.toBlocking().retrieve("/vets/specialities/" + speciality.id() + "/edit"))
                .contains("action=\"/vets/specialities/" + speciality.id() + "\"");
    }

    @Test
    void specialityFormsReturnJsonErrorsForDuplicatesInvalidNamesAndMissingIds() {
        SpecialityDto first = createSpeciality();
        SpecialityDto second = createSpeciality();
        assertJsonError(HttpRequest.POST("/vets/specialities", first), HttpStatus.BAD_REQUEST);
        String editUrl = "/vets/specialities/" + second.id();
        assertJsonError(HttpRequest.PUT(editUrl, Map.of("name", first.name())), HttpStatus.BAD_REQUEST);
        assertJsonError(HttpRequest.POST("/vets/specialities", Map.of("name", " ")), HttpStatus.BAD_REQUEST);
        assertJsonError(HttpRequest.PUT(editUrl, Map.of("name", " ")), HttpStatus.BAD_REQUEST);
        assertJsonError(HttpRequest.PUT("/vets/specialities/2147483647", Map.of("name", "unknown")), HttpStatus.NOT_FOUND);
        assertThat(specialities.findById(second.id()).orElseThrow().name()).isEqualTo(second.name());
    }

    @Test
    void formSupportsEmptySpecialitiesAndRejectsStaleSaves() {
        VetDto vet = createVet(Set.of(createSpeciality())).body();
        String url = "/vets/" + vet.id();
        String newPage = client.toBlocking().retrieve("/vets/new");
        assertThat(newPage).contains("action=\"/vets\"", "data-is-new=\"true\"", "/resources/js/vet-form.js");
        assertThat(client.toBlocking().retrieve("/vets?saved=true")).contains("Veterinarian saved.");
        String page = client.toBlocking().retrieve(url + "/edit");
        assertThat(page).contains("action=\"/vets/" + vet.id() + "\"", "name=\"etag\"", vet.etag(), "checked",
                "/resources/js/vet-form.js", "id=\"vet-feedback\"", "id=\"vet-delete-form\"")
                .doesNotContain("action=\"/vets/" + vet.id() + "/edit\"", "action=\"/vets/" + vet.id() + "/delete\"");
        VetDto form = new VetDto(vet.id(), "Form", "Updated", Set.of(), null);
        HttpResponse<VetDto> updated = client.toBlocking().exchange(HttpRequest.PUT(url, form)
                .header("If-Match", vet.etag()), VetDto.class);
        assertThat(updated.code()).isEqualTo(200);
        assertThat(updated.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(updated.header("Location")).isNull();
        assertThat(Objects.requireNonNull(updated.body()).id()).isEqualTo(vet.id());
        assertThat(Objects.requireNonNull(updated.body()).firstName()).isEqualTo("Form");
        assertThat(Objects.requireNonNull(updated.body()).lastName()).isEqualTo("Updated");
        assertThat(Objects.requireNonNull(updated.body()).specialities()).isEmpty();
        assertThat(Objects.requireNonNull(updated.body()).etag()).isNotEqualTo(vet.etag());
        assertThat(updated.header("ETag")).isEqualTo("\"" + Objects.requireNonNull(updated.body()).etag() + "\"");
        assertThat(vets.findById(vet.id()).orElseThrow().specialities()).isEmpty();
        assertJsonError(HttpRequest.PUT(url, form).header("If-Match", vet.etag()), HttpStatus.PRECONDITION_FAILED);
    }

    @Test
    void createsMultipleAssignmentsAndDeletesWithIfMatchWithoutABody() {
        SpecialityDto first = createSpeciality();
        SpecialityDto second = createSpeciality();
        VetDto vet = createVet(Set.of(first, second)).body();
        HttpResponse<String> deleted = client.toBlocking().exchange(HttpRequest.DELETE("/vets/" + vet.id())
                .header("If-Match", vet.etag()), String.class);
        assertThat(deleted.code()).isEqualTo(204);
        assertThat(deleted.header("Location")).isNull();
        assertThat(deleted.getBody()).isEmpty();
        assertThat(vets.findById(vet.id())).isEmpty();
        assertThat(specialities.findById(first.id())).isPresent();
        assertThat(specialities.findById(second.id())).isPresent();
    }

    @Test
    void rejectsWeakEtagsWithoutChangingTheVet() {
        HttpResponse<VetDto> created = createVet(Set.of(createSpeciality()));
        VetDto vet = created.body();
        String url = "/vets/" + vet.id();
        assertError(HttpRequest.PUT(url, vet).header("If-Match", "W/" + created.header("ETag")),
                HttpStatus.PRECONDITION_FAILED);
        assertThat(client.toBlocking().retrieve(url, VetDto.class)).isEqualTo(vet);
    }

    @Test
    void unchangedSaveRetainsTheEtagAndAssignments() {
        HttpResponse<VetDto> created = createVet(Set.of(createSpeciality()));
        VetDto vet = created.body();
        HttpResponse<VetDto> saved = client.toBlocking().exchange(HttpRequest.PUT("/vets/" + Objects.requireNonNull(vet).id(), new VetForm(vet.firstName(), vet.lastName(), new HashSet<>(vet.specialityIds())))
                .header("If-Match", Objects.requireNonNull(created.header("ETag"))), VetDto.class);
        assertThat(saved.body()).isEqualTo(vet);
        assertThat(saved.header("ETag")).isEqualTo(created.header("ETag"));
    }

    @Test
    void returnsNotFoundForUnknownVetsAndSpecialities() {
        assertError(HttpRequest.GET("/vets/2147483647"), HttpStatus.NOT_FOUND);
        assertError(HttpRequest.GET("/vets/2147483647/edit"), HttpStatus.NOT_FOUND);
        assertError(HttpRequest.PUT("/vets/specialities/2147483647", new SpecialityDto(null, "unknown")), HttpStatus.NOT_FOUND);
    }

    @Test
    void jsonMutationsReturnJsonForMissingResourcesAndInvalidInput() {
        assertJsonError(HttpRequest.PUT("/vets/2147483647", new VetDto(null, "Unknown", "Vet", Set.of(), null))
                .header("If-Match", "unknown"), HttpStatus.NOT_FOUND);
        assertJsonError(HttpRequest.DELETE("/vets/2147483647").header("If-Match", "unknown"), HttpStatus.NOT_FOUND);
        VetDto vet = createVet(Set.of()).body();
        assertJsonError(HttpRequest.PUT("/vets/" + vet.id(), new VetDto(vet.id(), "", "Vet", Set.of(), null))
                .header("If-Match", vet.etag()), HttpStatus.BAD_REQUEST);
    }

    @Test
    void jsonEditAcceptsSpecialityIds() {
        VetDto vet = createVet(Set.of()).body();
        SpecialityDto first = createSpeciality();
        SpecialityDto second = createSpeciality();
        String url = "/vets/" + Objects.requireNonNull(vet).id();
        HttpResponse<VetDto> response = client.toBlocking().exchange(HttpRequest.PUT(url,
                new VetForm("Selected", "Specialities", Set.of(first.id(), second.id())))
                .header("If-Match", vet.etag()), VetDto.class);
        assertThat(Objects.requireNonNull(response.body()).specialities()).containsExactlyInAnyOrder(first, second);
        assertThat(client.toBlocking().retrieve(url, VetDto.class)).isEqualTo(response.body());

    }

    @Test
    void rejectsNamesLongerThanTheDatabaseColumns() {
        String tooLong = "x".repeat(256);
        assertJsonError(HttpRequest.POST("/vets", new VetForm(tooLong, "Name", Set.of())), HttpStatus.BAD_REQUEST);
        VetDto vet = createVet(Set.of()).body();
        assertJsonError(HttpRequest.PUT("/vets/" + Objects.requireNonNull(vet).id(),
                new VetForm(vet.firstName(), tooLong, Set.of()))
                .header("If-Match", vet.etag()), HttpStatus.BAD_REQUEST);
        assertJsonError(HttpRequest.POST("/vets/specialities", Map.of("name", tooLong)), HttpStatus.BAD_REQUEST);
        SpecialityDto speciality = createSpeciality();
        assertJsonError(HttpRequest.PUT("/vets/specialities/" + speciality.id(), Map.of("name", tooLong)), HttpStatus.BAD_REQUEST);
        assertThat(client.toBlocking().retrieve("/vets/" + vet.id(), VetDto.class)).isEqualTo(vet);
    }

    @Test
    void malformedJsonAndInvalidPathIdsReturnBadRequests() {
        assertJsonError(HttpRequest.POST("/vets/specialities", "{\"name\":")
                .contentType(MediaType.APPLICATION_JSON), HttpStatus.BAD_REQUEST);
        assertJsonError(HttpRequest.GET("/vets/not-an-integer"), HttpStatus.BAD_REQUEST);
    }

    private void assertJsonError(HttpRequest<?> request, HttpStatus status) {
        HttpResponse<?> response = assertError(request, status).getResponse();
        assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(response.header("Location")).isNull();
        assertThat(response.getBody(String.class).orElseThrow()).contains("\"message\"").doesNotContain("<!DOCTYPE html>");
    }

    private HttpResponse<VetDto> createVet(Set<SpecialityDto> selected) {
        HttpResponse<VetDto> response = client.toBlocking().exchange(HttpRequest.POST("/vets",
                new VetForm("Review", "Test", selected.stream().map(SpecialityDto::id).collect(Collectors.toSet()))), VetDto.class);
        vetIds.add(Objects.requireNonNull(response.body()).id());
        return response;
    }

    private SpecialityDto createSpeciality() {
        SpecialityDto created = client.toBlocking().retrieve(HttpRequest.POST("/vets/specialities",
                new SpecialityDto(null, "review-" + UUID.randomUUID())), SpecialityDto.class);
        specialityIds.add(created.id());
        return created;
    }

    private HttpClientResponseException assertError(HttpRequest<?> request, HttpStatus status) {
        HttpClientResponseException error = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, String.class));
        assertThat((Object) error.getStatus()).isEqualTo(status);
        return error;
    }

}
