package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.createNoRedirectClient;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.exchange;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class VisitControllerTest {

    @Inject
    EmbeddedServer server;

    @Test
    void missingPetOnVisitSubmissionReturnsJsonEntityNotFound() {
        try (HttpClient client = createNoRedirectClient(server)) {
            String cookie = login(client, "admin@example.com", "password123");
            HttpResponse<String> response = exchange(client,
                    HttpRequest.POST("/owners/2147483647/pets/2147483647/visits/new", Map.of(
                                    "date", LocalDate.now().plusDays(1).toString(),
                                    "description", "Routine checkup",
                                    "durationMinutes", 30,
                                    "followUpPeriodMonths", 6))
                            .contentType(MediaType.APPLICATION_JSON_TYPE)
                            .accept(MediaType.APPLICATION_JSON_TYPE)
                            .header(HttpHeaders.COOKIE, cookie));

            assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
            assertThat(response.getBody(String.class).orElseThrow())
                    .contains("\"message\":\"Entity not found.\"");
        }
    }
}
