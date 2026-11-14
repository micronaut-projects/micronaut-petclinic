package io.micronaut.samples.petclinic.controller;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.controller.OwnerLoyaltyController;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for {@link OwnerController}.
 */
@MicronautTest
class OwnerControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ClinicServiceFixtures clinicServiceFixtures;

    @Inject
    ApplicationContext context;

    @Inject
    io.micronaut.samples.petclinic.repository.OwnerRepository ownerRepository;

    @Test
    void shouldReturnJsonCardDataWithoutRedirectsForAnyResultCount() {
        for (String lastName : java.util.List.of("Franklin", "NoSuchOwner")) {
            var response = client.toBlocking().exchange(
                    HttpRequest.GET("/owners/data?lastName=" + lastName)
                            .accept(io.micronaut.http.MediaType.APPLICATION_JSON_TYPE), java.util.Map.class);
            assertThat((Object) response.getStatus()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().get("Location")).isNull();
            var body = response.body();
            assertThat(body).hasSize(2).containsKeys("owners", "totalSize");
            var owners = (java.util.List<?>) body.get("owners");
            if (lastName.equals("Franklin")) {
                assertThat(owners).hasSize(1);
                var owner = (java.util.Map<?, ?>) owners.getFirst();
                assertThat(owner.get("lastName")).isEqualTo("Franklin");
                assertThat(owner.get("pets")).isEqualTo(java.util.List.of("Leo"));
            } else {
                assertThat(owners).isEmpty();
                assertThat(((Number) body.get("totalSize")).intValue()).isZero();
            }
        }
    }

    @Test
    void shouldReturnOnlyCardDataAndCountForTheRequestedPage() {
        var body = client.toBlocking().retrieve(
                "/owners/data?lastName=dAv&page=1&size=1&sort=id&direction=DESC", java.util.Map.class);
        assertThat(body).hasSize(2).containsKeys("owners", "totalSize");
        assertThat(((Number) body.get("totalSize")).intValue()).isEqualTo(2);
        assertThat((java.util.List<?>) body.get("owners")).hasSize(1);
    }

    @Test
    void shouldTranslateOwnerListControls() {
        var translations = java.util.Map.of(
                "en", java.util.List.of("Sort by", "Owners per page", "Apply", "Next"),
                "es", java.util.List.of("Ordenar por", "Propietarios por página", "Aplicar", "Siguiente"),
                "de", java.util.List.of("Sortieren nach", "Besitzer pro Seite", "Anwenden", "Weiter"));
        translations.forEach((language, labels) -> {
            String html = client.toBlocking().retrieve(HttpRequest.GET("/owners/list?size=1&sort=city&direction=desc")
                    .cookie(io.micronaut.http.cookie.Cookie.of("locale", language)), String.class);
            assertThat(html).contains(labels).doesNotContain("owner.sortBy", "owner.pagination", ">lastName</option>");
            assertThat(html).contains("owner-list-toolbar", "owner-pagination", "data-owner-pagination", "name=\"sort\"", "name=\"direction\"");
        });
    }

    @Test
    void shouldPageAndSortOwnerData() {
        var ordered = ownerRepository.findAll(io.micronaut.data.model.Sort.of(
                io.micronaut.data.model.Sort.Order.asc("id")));
        var first = ownerDataItems("/owners/data?size=2&sort=id");
        var later = ownerDataItems("/owners/data?page=1&size=2&sort=id");
        assertThat(first).hasSize(2);
        assertThat(((Number) first.get(0).get("id")).intValue()).isEqualTo(ordered.get(0).id());
        assertThat(((Number) first.get(1).get("id")).intValue()).isEqualTo(ordered.get(1).id());
        assertThat(((Number) later.get(0).get("id")).intValue()).isEqualTo(ordered.get(2).id());

        var descending = ownerDataItems("/owners/data?size=1&sort=id&direction=desc");
        assertThat(((Number) descending.getFirst().get("id")).intValue()).isEqualTo(ordered.getLast().id());
        assertThat(ownerDataItems("/owners/data")).hasSize(5);
    }

    @Test
    void shouldRenderListShellWithoutBackendPresentationState() {
        String html = client.toBlocking().retrieve("/owners/list?page=1&size=2&sort=id", String.class);
        assertThat(html).contains("data-owner-list", "data-owner-grid", "data-owner-card-template", "owner-list.js")
                .doesNotContain("/owners/" + clinicServiceFixtures.requiredOwner("George", "Franklin").id() + "\"");
    }

    @Test
    void shouldRejectInvalidPaginationParameters() {
        for (String route : java.util.List.of("/owners", "/owners/data")) {
            for (String query : java.util.List.of("page=-1", "size=0", "size=101", "page=nope",
                    "size=nope", "sort=pets.name", "direction=sideways")) {
                var exception = assertThrows(HttpClientResponseException.class,
                        () -> client.toBlocking().retrieve(route + "?" + query, String.class));
                assertThat((Object) exception.getStatus()).as(route + "?" + query).isEqualTo(HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Test
    void shouldPageFilteredOwnersAndHandleEmptyPages() {
        var first = ownerDataItems("/owners/data?lastName=dAv&size=1&sort=id");
        var later = ownerDataItems("/owners/data?lastName=dAv&size=1&page=1&sort=id");
        assertThat(first).hasSize(1);
        assertThat(later).hasSize(1);
        assertThat(first.getFirst().get("lastName")).isEqualTo("Davis");
        assertThat(later.getFirst().get("lastName")).isEqualTo("Davis");
        assertThat(first.getFirst().get("id")).isNotEqualTo(later.getFirst().get("id"));
        assertThat(ownerDataItems("/owners/data?page=999&size=1")).isEmpty();
    }

    @Test
    void shouldShowFindOwnersForm() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/owners/find"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Find Owners");
    }

    @Test
    void shouldShowOwnersList() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/owners/list"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Owners");
    }

    @Test
    void shouldFilterOwnersByLastName() {
        var owners = ownerDataItems("/owners/data?lastName=Davis");
        assertThat(owners).hasSize(2);
        owners.forEach(owner -> assertThat(owner.get("lastName")).isEqualTo("Davis"));
    }

    @Test
    void shouldShowOwnerDetails() {
        Owner owner = clinicServiceFixtures.requiredOwner("George", "Franklin");

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/owners/" + owner.id()), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("George");
        assertThat(response.body()).contains("Franklin");

        if (context.getEnvironment().getActiveNames().contains("oracle")) {
            assertThat(response.body()).contains("id=\"care-record-tab\"");
        } else {
            assertThat(response.body()).doesNotContain("id=\"care-record-tab\"");
            assertThat(response.body()).doesNotContain("id=\"owner-loyalty-card\"");
            assertThat(context.containsBean(OwnerLoyaltyController.class)).isFalse();
        }
        assertThat(response.body()).contains("id=\"export-care-summary\"");
        assertThat(response.body()).contains("href=\"/owners/" + owner.id() + "/care-summary\"");
    }

    @Test
    void shouldReturnNotFoundForAnUnknownOwner() {
        HttpClientResponseException failure = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.GET("/owners/2147483647"), String.class));

        assertThat((CharSequence) failure.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldShowNewOwnerForm() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/owners/new"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("New Owner");
    }

    @Test
    void shouldShowEditOwnerForm() {
        Owner owner = clinicServiceFixtures.requiredOwner("George", "Franklin");

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/owners/" + owner.id() + "/edit"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Edit Owner");
        assertThat(response.body()).contains("George");
    }

    @SuppressWarnings("unchecked")
    private java.util.List<java.util.Map<String, Object>> ownerDataItems(String uri) {
        var body = client.toBlocking().retrieve(uri, java.util.Map.class);
        return (java.util.List<java.util.Map<String, Object>>) body.get("owners");
    }
}
