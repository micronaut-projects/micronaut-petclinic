package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ClinicController}.
 */
@MicronautTest
class ClinicControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void shouldRenderClinicSearchPage() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/clinics"), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Find Clinics");
        assertThat(response.body()).contains("Manual");
        assertThat(response.body()).contains("Map");
        assertThat(response.body()).contains("<button class=\"nav-link active\" id=\"map-search-tab\"");
        assertThat(response.body()).contains("<div class=\"tab-pane fade show active\" id=\"map-search-panel\"");
        assertThat(response.body()).contains("class=\"clinic-map-instructions\"");
        assertThat(response.body()).contains("Click the map to choose a reference point");
        assertThat(response.body()).contains("clinic-reset-search");
        assertThat(response.body()).contains("id=\"clinic-example-line\"");
        assertThat(response.body()).contains("Example Line");
        assertThat(response.body()).contains("id=\"acceptingNewPatients\"");
        assertThat(response.body()).contains("id=\"emergencyService\"");
        assertThat(response.body()).contains("Accepting new patients");
        assertThat(response.body()).contains("Emergency service");
        assertThat(response.body()).contains("const exampleIntersectingLine = [");
        assertThat(response.body()).contains("[43.1100, -89.3300]");
        assertThat(response.body()).contains("function runClinicSearch()");
        assertThat(response.body()).contains("replaceManualPoints('line', exampleIntersectingLine.map(function (point) {");
        assertThat(response.body()).contains(">Reset");
        assertThat(response.body()).contains("<section id=\"clinic-results-section\" class=\"d-none\">");
        assertThat(response.body()).contains("Clinics Within Area");
        assertThat(response.body()).contains("Clinics Intersecting Line");
        assertThat(response.body()).contains("0 clinics matched");
        assertThat(response.body()).doesNotContain("Downtown Madison Pet Clinic");
    }

    @Test
    void shouldRenderClinicSearchPageWithoutRunningQueryParameterSearch() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/clinics?mode=near&latitude=43.0745&longitude=-89.3840&radiusMeters=350"), String.class);

        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("0 clinics matched");
        assertThat(response.body()).contains("<section id=\"clinic-results-section\" class=\"d-none\">");
        assertThat(response.body()).doesNotContain("Downtown Madison Pet Clinic");
    }
}
