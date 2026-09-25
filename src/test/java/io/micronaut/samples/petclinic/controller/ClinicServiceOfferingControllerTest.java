package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.samples.petclinic.repository.ClinicServiceOfferingRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for the clinic-owned service catalog showcase.
 */
@MicronautTest
class ClinicServiceOfferingControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ClinicServiceOfferingRepository offeringRepository;

    @AfterEach
    void removeDemoOffering() {
        offeringRepository.findByClinicIdAndServiceCode(1, "HTTP_DEMO")
                .ifPresent(offering -> offeringRepository.deleteById(offering.id()));
    }

    @Test
    void shouldRenderTheServiceCatalogForOneClinic() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/clinics/1/services"), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Services at Downtown Madison Pet Clinic");
        assertThat(response.body()).contains("How this example uses upsert");
        assertThat(response.body()).contains("New service offering");
        assertThat(response.body()).contains("/clinics/1/services/new");
        assertThat(response.body()).contains("WELLNESS_CHECK");
    }

    @Test
    void shouldRenderTheClinicScopedCreateForm() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/clinics/2/services/new"), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("New service offering");
        assertThat(response.body()).contains("Capitol Square Pet Clinic");
        assertThat(response.body()).contains("The code is unique within this clinic");
    }

    @Test
    void shouldRenderTheServiceCatalogInSpanish() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/clinics/1/services")
                        .cookie(Cookie.of("locale", "es")), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body())
                .contains("Servicios en Downtown Madison Pet Clinic")
                .contains("Nuevo servicio")
                .contains("Código del servicio")
                .contains("Cómo utiliza este ejemplo upsert")
                .doesNotContain("New service offering");
    }

    @Test
    void shouldUseUpsertForCreateAndRepeatSubmissionFromTheForm() {
        String firstSubmission = """
                {
                  "serviceCode": "HTTP_DEMO",
                  "name": "Initial service",
                  "description": "Initial description",
                  "price": 40.00,
                  "durationMinutes": 30
                }
                """;
        String secondSubmission = """
                {
                  "serviceCode": "HTTP_DEMO",
                  "name": "Updated service",
                  "description": "Updated description",
                  "price": 60.00,
                  "durationMinutes": 45
                }
                """;

        HttpResponse<String> firstResponse = client.toBlocking().exchange(
                HttpRequest.POST("/clinics/1/services", firstSubmission)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON),
                String.class);
        HttpResponse<String> secondResponse = client.toBlocking().exchange(
                HttpRequest.POST("/clinics/1/services", secondSubmission)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON),
                String.class);

        assertThat((CharSequence) firstResponse.status()).isEqualTo(HttpStatus.OK);
        assertThat((CharSequence) secondResponse.status()).isEqualTo(HttpStatus.OK);
        assertThat(firstResponse.body()).contains("\"serviceCode\":\"HTTP_DEMO\"");
        assertThat(secondResponse.body()).contains("\"name\":\"Updated service\"");
        assertThat(offeringRepository.findByClinicIdAndServiceCode(1, "HTTP_DEMO"))
                .get()
                .satisfies(offering -> {
                    assertThat(offering.name()).isEqualTo("Updated service");
                    assertThat(offering.price()).isEqualByComparingTo("60.00");
                    assertThat(offering.durationMinutes()).isEqualTo(45);
                });
    }

    @Test
    void shouldReturnJsonValidationErrorsForJsonRequests() {
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.POST("/clinics/1/services", """
                                {
                                  "serviceCode": "",
                                  "name": "",
                                  "price": -1,
                                  "durationMinutes": 0
                                }
                                """)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON),
                        String.class));

        assertThat((CharSequence) exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        String responseBody = exception.getResponse().getBody(String.class).orElseThrow();
        assertThat(responseBody)
                .contains("\"message\":\"Validation failed\"")
                .contains("serviceCode");
    }
}
