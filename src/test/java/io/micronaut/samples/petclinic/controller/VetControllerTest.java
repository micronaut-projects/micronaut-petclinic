package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.repository.VetRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.createNoRedirectClient;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.exchange;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.formPost;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link VetController}.
 */
@MicronautTest
class VetControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    EmbeddedServer server;

    @Inject
    VetRepository vetRepository;

    @Test
    void shouldReturnVetsJson() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/vets/json"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).isNotNull();
        assertThat(response.body()).contains("firstName");
        assertThat(response.body()).contains("lastName");
    }

    @Test
    void shouldReturnVetsHtmlPage() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/vets"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).isNotNull();
        assertThat(response.body()).contains("Veterinarians");
    }

    @Test
    void shouldReturnNewVetForm() {
        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            String sessionCookie = login(noRedirectClient, "admin@example.com", "password123");

            HttpResponse<String> response = exchange(noRedirectClient, HttpRequest.GET("/vets/new")
                    .header(HttpHeaders.COOKIE, sessionCookie));

            assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
            assertThat(response.body()).contains("New Veterinarian");
        }
    }

    @Test
    void shouldRejectAnonymousVetCreation() {
        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            HttpResponse<String> response = exchange(noRedirectClient, formPost("/vets/new", Map.of(
                    "firstName", "Anonymous",
                    "lastName", "Vet"
            )));

            assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    void shouldCreateVetWhenAuthenticated() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String firstName = "Test" + suffix;
        String lastName = "Vet" + suffix;

        try (HttpClient noRedirectClient = createNoRedirectClient(server)) {
            String sessionCookie = login(noRedirectClient, "admin@example.com", "password123");

            HttpResponse<String> response = exchange(noRedirectClient, formPost("/vets/new", Map.of(
                    "firstName", firstName,
                    "lastName", lastName
            )).header(HttpHeaders.COOKIE, sessionCookie));

            assertThat(response.status().getCode()).isBetween(300, 399);
            assertThat(response.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo("/vets");
        }

        Iterable<Vet> vets = vetRepository.findAll();
        assertThat(vets).anySatisfy(vet -> {
            assertThat(vet.getFirstName()).isEqualTo(firstName);
            assertThat(vet.getLastName()).isEqualTo(lastName);
        });
    }
}
