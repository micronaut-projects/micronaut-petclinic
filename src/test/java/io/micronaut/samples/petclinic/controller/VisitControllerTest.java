package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class VisitControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void missingPetOnVisitSubmissionReturnsJsonEntityNotFound() {
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.POST("/owners/2147483647/pets/2147483647/visits/new", """
                                        {
                                          "date": "2025-01-01",
                                          "description": "Routine checkup",
                                          "durationMinutes": 30,
                                          "followUpPeriodMonths": 6
                                        }
                                        """)
                                .contentType(MediaType.APPLICATION_JSON_TYPE)
                                .accept(MediaType.APPLICATION_JSON_TYPE),
                        String.class));

        assertThat((CharSequence) exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_TYPE);
        assertThat(exception.getResponse().getBody(String.class).orElseThrow())
                .contains("\"message\":\"Entity not found.\"");
    }
}
