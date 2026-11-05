package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.BasicHttpAttributes;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** HTTP coverage for pet form validation and preservation of route identity. */
@MicronautTest
class PetControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ClinicServiceFixtures fixtures;

    @Inject
    ClinicService clinicService;

    @Inject
    PetController controller;

    @Test
    void missingUriTemplateReturnsNotFoundFromValidationHandler() {
        var response = controller.onConstraintViolation(HttpRequest.POST("/owners/1/pets/new", ""),
                new ConstraintViolationException(Set.of()));

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void missingOwnerPathVariableReturnsNotFoundFromValidationHandler() {
        var request = HttpRequest.POST("/pets/new", "");
        BasicHttpAttributes.setUriTemplate(request, "/pets/new");

        var response = controller.onConstraintViolation(request, new ConstraintViolationException(Set.of()));

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void invalidNewPetKeepsItsOwnerAndSubmittedFieldsDespiteConflictingQueryIds() {
        var owner = fixtures.requiredOwner("George", "Franklin");
        var type = clinicService.findPetTypes().iterator().next();
        String action = "/owners/" + owner.id() + "/pets/new";

        var failure = submitInvalidPet(action + "?ownerId=2147483647&petId=2147483647",
                "name=&birthDate=2024-02-03&typeId=" + type.id());

        assertThat((CharSequence) failure.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        String page = failure.getResponse().getBody(String.class).orElseThrow();
        assertThat(page).contains("George", "Franklin", "Pet name",
                "action=\"" + action + "\"", "value=\"2024-02-03\"",
                "<option value=\"" + type.id() + "\" selected>");
    }

    @Test
    void invalidEditKeepsThePathPetIdAndDoesNotPersistSubmittedChanges() throws Exception {
        var owner = fixtures.requiredOwner("George", "Franklin");
        var pet = fixtures.requiredPet("George", "Franklin", "Leo");
        var type = clinicService.findPetTypes().stream()
                .filter(candidate -> !candidate.id().equals(pet.type().id()))
                .findFirst().orElseThrow();
        String action = "/owners/" + owner.id() + "/pets/" + pet.id() + "/edit";

        var failure = submitInvalidPet(action,
                "id=2147483647&name=&birthDate=2024-02-03&typeId=" + type.id());

        assertThat((CharSequence) failure.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        String page = failure.getResponse().getBody(String.class).orElseThrow();
        assertThat(page).contains("George", "Franklin", "Pet name",
                "action=\"" + action + "\"", "value=\"2024-02-03\"",
                "<option value=\"" + type.id() + "\" selected>");
        var unchanged = clinicService.findPetById(pet.id());
        assertThat(unchanged.name()).isEqualTo(pet.name());
        assertThat(unchanged.birthDate()).isEqualTo(pet.birthDate());
        assertThat(unchanged.type().id()).isEqualTo(pet.type().id());
    }

    @Test
    void invalidPetForAnUnknownOwnerReturnsNotFound() {
        var failure = submitInvalidPet("/owners/2147483647/pets/new",
                "name=&birthDate=2024-02-03&typeId=1");

        assertThat((CharSequence) failure.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpClientResponseException submitInvalidPet(String uri, String form) {
        var failure = assertThrows(HttpClientResponseException.class, () -> client.toBlocking().exchange(
                HttpRequest.POST(uri, form)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                        .accept(MediaType.ALL_TYPE)
                        .cookie(Cookie.of("locale", "en")), String.class));
        assertThat(failure.getResponse().getContentType()).contains(MediaType.TEXT_HTML_TYPE);
        return failure;
    }
}
