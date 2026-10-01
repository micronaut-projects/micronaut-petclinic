package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Geospatial controller integration tests.
 */
@MicronautTest
class ClinicControllerGeoTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void shouldReturnClinicsWithinBoundingBoxAsJson() {
        String polygon = """
                {
                  "coordinates": [
                    {"latitude": 43.00, "longitude": -89.55},
                    {"latitude": 43.20, "longitude": -89.55},
                    {"latitude": 43.20, "longitude": -89.20},
                    {"latitude": 43.00, "longitude": -89.20},
                    {"latitude": 43.00, "longitude": -89.55}
                  ]
                }
                """;

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.POST("/clinics/within", polygon)
                        .contentType(MediaType.APPLICATION_JSON), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Downtown Madison Pet Clinic");
        assertThat(response.body()).doesNotContain("Milwaukee Pet Clinic");
    }

    @Test
    void shouldReturnClinicsWithinDrawnPolygonAsJson() {
        String polygon = """
                {
                  "coordinates": [
                    {"latitude": 43.00, "longitude": -89.55},
                    {"latitude": 43.20, "longitude": -89.55},
                    {"latitude": 43.20, "longitude": -89.20},
                    {"latitude": 43.00, "longitude": -89.20},
                    {"latitude": 43.00, "longitude": -89.55}
                  ]
                }
                """;

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.POST("/clinics/within", polygon)
                        .contentType(MediaType.APPLICATION_JSON), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Downtown Madison Pet Clinic");
        assertThat(response.body()).contains("Monona Pet Clinic");
        assertThat(response.body()).doesNotContain("Milwaukee Pet Clinic");
    }

    @Test
    void shouldReturnClinicsWithinConcavePolygonAsJson() {
        String polygon = """
                {
                  "coordinates": [
                    {"latitude": 43.0735, "longitude": -89.3860},
                    {"latitude": 43.0755, "longitude": -89.3860},
                    {"latitude": 43.0755, "longitude": -89.3825},
                    {"latitude": 43.0746, "longitude": -89.3825},
                    {"latitude": 43.0746, "longitude": -89.3835},
                    {"latitude": 43.0735, "longitude": -89.3835},
                    {"latitude": 43.0735, "longitude": -89.3860}
                  ]
                }
                """;

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.POST("/clinics/within", polygon)
                        .contentType(MediaType.APPLICATION_JSON), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Downtown Madison Pet Clinic");
        assertThat(response.body()).contains("Capitol Square Pet Clinic");
        assertThat(response.body()).doesNotContain("University Pet Clinic");
        assertThat(response.body()).doesNotContain("Milwaukee Pet Clinic");
    }

    @Test
    void shouldReturnClinicsIntersectingLineAsJson() {
        String line = """
                {
                  "coordinates": [
                    {"latitude": 43.0650, "longitude": -89.5500},
                    {"latitude": 43.1100, "longitude": -89.3300},
                    {"latitude": 43.1900, "longitude": -89.2000}
                  ]
                }
                """;

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.POST("/clinics/intersects", line)
                        .contentType(MediaType.APPLICATION_JSON), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("West Madison Pet Clinic");
        assertThat(response.body()).contains("East Madison Pet Clinic");
        assertThat(response.body()).contains("Sun Prairie Pet Clinic");
        assertThat(response.body()).doesNotContain("Downtown Madison Pet Clinic");
        assertThat(response.body()).doesNotContain("Janesville Pet Clinic");
    }

    @Test
    void shouldReturnClinicsIntersectingDrawnMultiPointLineAsJson() {
        String line = """
                {
                  "coordinates": [
                    {"latitude": 43.0650, "longitude": -89.5500},
                    {"latitude": 43.1100, "longitude": -89.3300},
                    {"latitude": 43.1900, "longitude": -89.2000}
                  ]
                }
                """;

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.POST("/clinics/intersects", line)
                        .contentType(MediaType.APPLICATION_JSON), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("West Madison Pet Clinic");
        assertThat(response.body()).contains("East Madison Pet Clinic");
        assertThat(response.body()).contains("Sun Prairie Pet Clinic");
        assertThat(response.body()).doesNotContain("Downtown Madison Pet Clinic");
    }

    @Test
    void shouldReturnClinicsIntersectingDrawnLineAsJson() {
        String line = """
                {
                  "coordinates": [
                    {"latitude": 43.0650, "longitude": -89.5500},
                    {"latitude": 43.1900, "longitude": -89.2000}
                  ]
                }
                """;

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.POST("/clinics/intersects", line)
                        .contentType(MediaType.APPLICATION_JSON), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("West Madison Pet Clinic");
        assertThat(response.body()).contains("Sun Prairie Pet Clinic");
        assertThat(response.body()).doesNotContain("Downtown Madison Pet Clinic");
    }

    @Test
    void shouldReturnNearbyClinicsAsJson() {
        String nearby = """
                {
                  "latitude": 43.0745,
                  "longitude": -89.3840,
                  "radiusMeters": 350
                }
                """;

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.POST("/clinics/nearby", nearby)
                        .contentType(MediaType.APPLICATION_JSON), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Downtown Madison Pet Clinic");
        assertThat(response.body()).contains("Capitol Square Pet Clinic");
        assertThat(response.body()).contains("\"latitude\":43.0748");
        assertThat(response.body()).doesNotContain("University Pet Clinic");
    }

    @Test
    void shouldReturnNearbyClinicsFilteredByBooleanFlags() {
        String nearby = """
                {
                  "latitude": 43.0745,
                  "longitude": -89.3840,
                  "radiusMeters": 5000,
                  "acceptingNewPatients": true,
                  "emergencyService": true
                }
                """;

        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.POST("/clinics/nearby", nearby)
                        .contentType(MediaType.APPLICATION_JSON), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Downtown Madison Pet Clinic");
        assertThat(response.body()).contains("East Madison Pet Clinic");
        assertThat(response.body()).contains("\"acceptingNewPatients\":true");
        assertThat(response.body()).contains("\"emergencyService\":true");
        assertThat(response.body()).doesNotContain("Capitol Square Pet Clinic");
        assertThat(response.body()).doesNotContain("University Pet Clinic");
    }
}
