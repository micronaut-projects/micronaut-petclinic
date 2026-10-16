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
import io.micronaut.samples.petclinic.repository.SpecialityRepository;
import io.micronaut.samples.petclinic.repository.VetRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(transactional = false)
@Property(name = "micronaut.http.client.follow-redirects", value = "false")
class VetCrudTest {
    @Inject @Client("/") HttpClient client;
    @Inject VetRepository vets;
    @Inject SpecialityRepository specialities;

    private final List<Integer> vetIds = new ArrayList<>();
    private final List<Integer> specialityIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        vetIds.forEach(id -> vets.findById(id).ifPresent(vets::delete));
        specialityIds.forEach(specialities::deleteById);
    }

    @Test
    void createsReadsUpdatesAndDeletesUsingQuotedEtags() {
        SpecialityDto speciality = createSpeciality();
        HttpResponse<VetDto> created = createVet(Set.of(speciality));
        VetDto vet = created.body();
        String url = "/vets/" + vet.id();
        String etag = created.header("ETag");
        assertThat((Object) created.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(etag).isEqualTo("\"" + vet.etag() + "\"");
        assertThat(created.header("Cache-Control")).isEqualTo("no-store");

        HttpResponse<VetDto> loaded = client.toBlocking().exchange(HttpRequest.GET(url), VetDto.class);
        assertThat(loaded.body().specialities()).containsExactly(speciality);
        assertThat(loaded.header("ETag")).isEqualTo(etag);

        HttpResponse<VetDto> updated = client.toBlocking().exchange(HttpRequest.PUT(url,
                new VetDto(vet.id(), "Updated", vet.lastName(), Set.of(), null)).header("If-Match", etag), VetDto.class);
        assertThat(updated.body().firstName()).isEqualTo("Updated");
        assertThat(updated.header("ETag")).isNotEqualTo(etag);
        assertThat(vets.findById(vet.id()).orElseThrow().specialities()).isEmpty();
        assertThat(specialities.findById(speciality.id())).isPresent();

        HttpResponse<?> deleted = client.toBlocking().exchange(HttpRequest.DELETE(url)
                .header("If-Match", updated.header("ETag")));
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
        VetDto vet = created.body();
        String url = "/vets/" + vet.id();
        HttpResponse<VetDto> updated = client.toBlocking().exchange(HttpRequest.PUT(url,
                new VetDto(vet.id(), "Winner", vet.lastName(), Set.of(second), null))
                .header("If-Match", created.header("ETag")), VetDto.class);
        assertThat(updated.body().specialities()).containsExactly(second);

        assertError(HttpRequest.PUT(url, new VetDto(vet.id(), "Loser", "Changed", Set.of(first), null))
                .header("If-Match", created.header("ETag")), HttpStatus.PRECONDITION_FAILED);
        assertError(HttpRequest.DELETE(url).header("If-Match", created.header("ETag")), HttpStatus.PRECONDITION_FAILED);
        VetDto current = client.toBlocking().retrieve(url, VetDto.class);
        assertThat(current).isEqualTo(updated.body());
    }

    @Test
    void requiresAnEtagForJsonAndFormMutations() {
        VetDto vet = createVet(Set.of()).body();
        String url = "/vets/" + vet.id();
        assertError(HttpRequest.PUT(url, vet), HttpStatus.PRECONDITION_REQUIRED);
        assertError(HttpRequest.DELETE(url), HttpStatus.PRECONDITION_REQUIRED);
        assertJsonError(HttpRequest.POST(url + "/edit", new VetForm(vet.id(), "Other", "Name", Set.of(), null)), HttpStatus.PRECONDITION_REQUIRED);
        assertJsonError(HttpRequest.DELETE(url + "/delete"), HttpStatus.PRECONDITION_REQUIRED);
        assertJsonError(HttpRequest.DELETE(url + "/delete").header("If-Match", ""), HttpStatus.PRECONDITION_REQUIRED);
        assertJsonError(HttpRequest.DELETE(url + "/delete", Map.of("etag", vet.etag())), HttpStatus.PRECONDITION_REQUIRED);
        assertThat(vets.findById(vet.id())).isPresent();
    }

    @Test
    void rejectsUnknownSpecialitiesWithAnActionableBadRequest() {
        HttpClientResponseException error = assertError(HttpRequest.POST("/vets",
                new VetDto(null, "Unknown", "Speciality", Set.of(new SpecialityDto(Integer.MAX_VALUE, "unknown")), null)),
                HttpStatus.BAD_REQUEST);
        assertThat(error.getResponse().getBody(String.class).orElseThrow()).contains("specialities");
    }

    @Test
    void rejectsBlankNames() {
        assertError(HttpRequest.POST("/vets", new VetDto(null, " ", "Name", Set.of(), null)), HttpStatus.BAD_REQUEST);
        assertError(HttpRequest.POST("/vets/specialities", new SpecialityDto(null, " ")), HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsSpecialitiesWithoutAnIdentifier() {
        assertError(HttpRequest.POST("/vets", new VetDto(null, "Review", "MissingId",
                Set.of(new SpecialityDto(null, "missing")), null)), HttpStatus.BAD_REQUEST);
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
                .header("If-Match", first.header("ETag")));
        assertThat(vets.findById(first.body().id())).isEmpty();
        assertThat(client.toBlocking().retrieve("/vets/" + second.id(), VetDto.class).specialities()).containsExactly(speciality);
    }

    @Test
    void staleFormUpdatesRollBackSpecialityLinkChanges() {
        SpecialityDto speciality = createSpeciality();
        HttpResponse<VetDto> created = createVet(Set.of(speciality));
        VetDto vet = created.body();
        String url = "/vets/" + vet.id();
        client.toBlocking().exchange(HttpRequest.PUT(url,
                new VetDto(vet.id(), "Winner", vet.lastName(), Set.of(speciality), null))
                .header("If-Match", created.header("ETag")), VetDto.class);
        assertJsonError(HttpRequest.POST(url + "/edit",
                new VetForm(vet.id(), "Loser", "Changed", Set.of(), vet.etag())), HttpStatus.PRECONDITION_FAILED);
        assertJsonError(HttpRequest.DELETE(url + "/delete").header("If-Match", created.header("ETag")), HttpStatus.PRECONDITION_FAILED);
        VetDto current = client.toBlocking().retrieve(url, VetDto.class);
        assertThat(current.firstName()).isEqualTo("Winner");
        assertThat(current.specialities()).containsExactly(speciality);
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
                HttpRequest.POST("/vets/specialities/new", Map.of("name", name)), SpecialityDto.class);
        SpecialityDto speciality = created.body();
        specialityIds.add(speciality.id());
        assertThat(created.code()).isEqualTo(201);
        assertThat(created.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(created.header("Location")).isNull();
        assertThat(speciality.name()).isEqualTo(name);
        VetDto vet = createVet(Set.of(speciality)).body();

        String editUrl = "/vets/specialities/" + speciality.id() + "/edit";
        HttpResponse<SpecialityDto> unchanged = client.toBlocking().exchange(
                HttpRequest.POST(editUrl, speciality), SpecialityDto.class);
        assertThat(unchanged.body()).isEqualTo(speciality);
        HttpResponse<SpecialityDto> updated = client.toBlocking().exchange(
                HttpRequest.POST(editUrl, Map.of("name", name + " renamed")), SpecialityDto.class);
        assertThat(updated.code()).isEqualTo(200);
        assertThat(updated.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(updated.header("Location")).isNull();
        assertThat(updated.body()).isEqualTo(new SpecialityDto(speciality.id(), name + " renamed"));
        assertThat(client.toBlocking().retrieve("/vets/" + vet.id(), VetDto.class).specialities())
                .containsExactly(updated.body());
        assertThat(client.toBlocking().retrieve("/vets/specialities/new"))
                .contains("id=\"speciality-form\"", "/resources/js/speciality-form.js");
        assertThat(client.toBlocking().retrieve(editUrl + "?saved=true"))
                .contains("id=\"speciality-feedback\"", "Speciality saved.", name + " renamed");
    }

    @Test
    void specialityFormsReturnJsonErrorsForDuplicatesInvalidNamesAndMissingIds() {
        SpecialityDto first = createSpeciality();
        SpecialityDto second = createSpeciality();
        assertJsonError(HttpRequest.POST("/vets/specialities/new", first), HttpStatus.BAD_REQUEST);
        String editUrl = "/vets/specialities/" + second.id() + "/edit";
        assertJsonError(HttpRequest.POST(editUrl, Map.of("name", first.name())), HttpStatus.BAD_REQUEST);
        assertJsonError(HttpRequest.POST("/vets/specialities/new", Map.of("name", " ")), HttpStatus.BAD_REQUEST);
        assertJsonError(HttpRequest.POST(editUrl, Map.of("name", " ")), HttpStatus.BAD_REQUEST);
        assertJsonError(HttpRequest.POST("/vets/specialities/2147483647/edit", Map.of("name", "unknown")), HttpStatus.NOT_FOUND);
        assertThat(specialities.findById(second.id()).orElseThrow().name()).isEqualTo(second.name());
    }

    @Test
    void formSupportsEmptySpecialitiesAndRejectsStaleSaves() {
        VetDto vet = createVet(Set.of(createSpeciality())).body();
        String url = "/vets/" + vet.id();
        String page = client.toBlocking().retrieve(url + "/edit");
        assertThat(page).contains("name=\"etag\"", vet.etag(), "checked", "/resources/js/vet-form.js", "id=\"vet-feedback\"", "id=\"vet-delete-form\"");
        VetForm form = new VetForm(vet.id(), "Form", "Updated", Set.of(), vet.etag());
        HttpResponse<VetDto> updated = client.toBlocking().exchange(HttpRequest.POST(url + "/edit", form), VetDto.class);
        assertThat(updated.code()).isEqualTo(200);
        assertThat(updated.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(updated.header("Location")).isNull();
        assertThat(updated.body().id()).isEqualTo(vet.id());
        assertThat(updated.body().firstName()).isEqualTo("Form");
        assertThat(updated.body().lastName()).isEqualTo("Updated");
        assertThat(updated.body().specialities()).isEmpty();
        assertThat(updated.body().etag()).isNotEqualTo(vet.etag());
        assertThat(updated.header("ETag")).isEqualTo("\"" + updated.body().etag() + "\"");
        assertThat(vets.findById(vet.id()).orElseThrow().specialities()).isEmpty();
        assertJsonError(HttpRequest.POST(url + "/edit", form), HttpStatus.PRECONDITION_FAILED);
    }

    @Test
    void formCreatesMultipleAssignmentsAndDeletesWithIfMatchWithoutABody() {
        SpecialityDto first = createSpeciality();
        SpecialityDto second = createSpeciality();
        String lastName = "form-" + UUID.randomUUID();
        assertThat(client.toBlocking().retrieve("/vets/new")).contains("name=\"firstName\"", "name=\"specialityIds\"");
        HttpResponse<String> created = client.toBlocking().exchange(HttpRequest.POST("/vets/new",
                "firstName=Form&lastName=" + lastName + "&specialityIds=" + first.id() + "&specialityIds=" + second.id())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED), String.class);
        assertThat(created.header("Location")).isEqualTo("/vets?saved=true");
        var vet = vets.findAll().stream().filter(value -> value.lastName().equals(lastName)).findFirst().orElseThrow();
        vetIds.add(vet.id());
        assertThat(vet.specialities()).extracting(value -> value.id()).containsExactlyInAnyOrder(first.id(), second.id());
        HttpResponse<String> deleted = client.toBlocking().exchange(HttpRequest.DELETE("/vets/" + vet.id() + "/delete")
                .header("If-Match", "\"" + vet.etag() + "\""), String.class);
        assertThat(deleted.code()).isEqualTo(204);
        assertThat(deleted.header("Location")).isNull();
        assertThat(deleted.getBody()).isEmpty();
        assertThat(vets.findById(vet.id())).isEmpty();
        assertThat(specialities.findById(first.id())).isPresent();
        assertThat(specialities.findById(second.id())).isPresent();
    }

    @Test
    void rejectsWeakEtagsAndMismatchedFormIdsWithoutChangingTheVet() {
        HttpResponse<VetDto> created = createVet(Set.of(createSpeciality()));
        VetDto vet = created.body();
        String url = "/vets/" + vet.id();
        assertError(HttpRequest.PUT(url, vet).header("If-Match", "W/" + created.header("ETag")),
                HttpStatus.PRECONDITION_FAILED);
        assertJsonError(HttpRequest.POST(url + "/edit", new VetForm(Integer.MAX_VALUE, "Changed", "Name", Set.of(), vet.etag())), HttpStatus.BAD_REQUEST);
        assertThat(client.toBlocking().retrieve(url, VetDto.class)).isEqualTo(vet);
    }

    @Test
    void unchangedSaveRetainsTheEtagAndAssignments() {
        HttpResponse<VetDto> created = createVet(Set.of(createSpeciality()));
        VetDto vet = created.body();
        HttpResponse<VetDto> saved = client.toBlocking().exchange(HttpRequest.PUT("/vets/" + vet.id(), vet)
                .header("If-Match", created.header("ETag")), VetDto.class);
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
    void formMutationsReturnJsonForMissingResourcesAndInvalidInput() {
        assertJsonError(HttpRequest.POST("/vets/2147483647/edit", new VetForm(null, "Unknown", "Vet", Set.of(), "unknown")), HttpStatus.NOT_FOUND);
        assertJsonError(HttpRequest.DELETE("/vets/2147483647/delete").header("If-Match", "\"unknown\""), HttpStatus.NOT_FOUND);
        VetDto vet = createVet(Set.of()).body();
        assertJsonError(HttpRequest.POST("/vets/" + vet.id() + "/edit", new VetForm(vet.id(), "", "Vet", Set.of(), vet.etag())), HttpStatus.BAD_REQUEST);
    }

    @Test
    void jsonEditAcceptsSpecialityIds() {
        VetDto vet = createVet(Set.of()).body();
        SpecialityDto first = createSpeciality();
        SpecialityDto second = createSpeciality();
        String url = "/vets/" + vet.id();
        HttpResponse<VetDto> response = client.toBlocking().exchange(HttpRequest.POST(url + "/edit",
                new VetForm(vet.id(), "Selected", "Specialities", Set.of(first.id(), second.id()), vet.etag())), VetDto.class);
        assertThat(response.body().specialities()).containsExactlyInAnyOrder(first, second);
        assertThat(client.toBlocking().retrieve(url, VetDto.class)).isEqualTo(response.body());

    }

    @Test
    void rejectsNamesLongerThanTheDatabaseColumns() {
        String tooLong = "x".repeat(256);
        assertJsonError(HttpRequest.POST("/vets", new VetDto(null, tooLong, "Name", Set.of(), null)), HttpStatus.BAD_REQUEST);
        VetDto vet = createVet(Set.of()).body();
        assertJsonError(HttpRequest.POST("/vets/" + vet.id() + "/edit",
                new VetForm(vet.id(), vet.firstName(), tooLong, Set.of(), vet.etag())), HttpStatus.BAD_REQUEST);
        assertJsonError(HttpRequest.POST("/vets/specialities/new", Map.of("name", tooLong)), HttpStatus.BAD_REQUEST);
        SpecialityDto speciality = createSpeciality();
        assertJsonError(HttpRequest.POST("/vets/specialities/" + speciality.id() + "/edit", Map.of("name", tooLong)), HttpStatus.BAD_REQUEST);
        assertThat(client.toBlocking().retrieve("/vets/" + vet.id(), VetDto.class)).isEqualTo(vet);
    }

    @Test
    void malformedJsonAndInvalidPathIdsReturnBadRequests() {
        assertJsonError(HttpRequest.POST("/vets/specialities/new", "{\"name\":")
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
                new VetDto(null, "Review", "Test", selected, null)), VetDto.class);
        vetIds.add(response.body().id());
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
