package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.createNoRedirectClient;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.exchange;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.formPost;
import static io.micronaut.samples.petclinic.controller.ControllerTestSupport.login;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for role-based controller authorization.
 */
@MicronautTest
class AuthorizationControllerTest {

    @Inject
    EmbeddedServer server;

    @Inject
    ClinicServiceFixtures clinicServiceFixtures;

    @Test
    void shouldAllowStaffButRejectAdminForStaffOnlyOwnerCreation() {
        try (HttpClient client = createNoRedirectClient(server)) {
            String staffCookie = login(client, "staff@example.com", "password123");
            String adminCookie = login(client, "admin@example.com", "password123");

            HttpResponse<String> staffResponse = exchange(client, formPost("/owners/new", ownerForm("StaffOnly"))
                    .header(HttpHeaders.COOKIE, staffCookie));
            HttpResponse<String> adminResponse = exchange(client, formPost("/owners/new", ownerForm("AdminDenied"))
                    .header(HttpHeaders.COOKIE, adminCookie));

            assertThat(staffResponse.status().getCode()).isBetween(300, 399);
            assertThat(staffResponse.getHeaders().get(HttpHeaders.LOCATION)).startsWith("/owners/");
            assertThat((CharSequence) adminResponse.status()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void shouldAllowStaffAndAdminForOwnerUpdates() {
        try (HttpClient client = createNoRedirectClient(server)) {
            String staffCookie = login(client, "staff@example.com", "password123");
            String adminCookie = login(client, "admin@example.com", "password123");

            String suffix = UUID.randomUUID().toString().substring(0, 8);
            HttpResponse<String> staffOwnerCreated = exchange(client,
                    formPost("/owners/new", ownerForm("StaffOwner" + suffix))
                            .header(HttpHeaders.COOKIE, staffCookie));
            HttpResponse<String> adminOwnerCreated = exchange(client,
                    formPost("/owners/new", ownerForm("AdminOwner" + suffix))
                            .header(HttpHeaders.COOKIE, staffCookie));
            String staffOwnerPath = staffOwnerCreated.getHeaders().get(HttpHeaders.LOCATION);
            String adminOwnerPath = adminOwnerCreated.getHeaders().get(HttpHeaders.LOCATION);
            assertThat(staffOwnerCreated.status().getCode()).isBetween(300, 399);
            assertThat(adminOwnerCreated.status().getCode()).isBetween(300, 399);
            assertThat(staffOwnerPath).isNotBlank();
            assertThat(adminOwnerPath).isNotBlank();

            HttpResponse<String> staffResponse = exchange(client, formPost(staffOwnerPath + "/edit", ownerForm("StaffUpdate"))
                    .header(HttpHeaders.COOKIE, staffCookie));
            HttpResponse<String> adminResponse = exchange(client, formPost(adminOwnerPath + "/edit", ownerForm("AdminUpdate"))
                    .header(HttpHeaders.COOKIE, adminCookie));

            assertThat(staffResponse.status().getCode()).isBetween(300, 399);
            assertThat(staffResponse.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo(staffOwnerPath);
            assertThat(adminResponse.status().getCode()).isBetween(300, 399);
            assertThat(adminResponse.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo(adminOwnerPath);
        }
    }

    @Test
    void shouldAllowAuthenticatedUserWithoutRolesForVisitCreation() {
        String username = "auth-only-" + UUID.randomUUID() + "@example.com";
        String password = "password123";

        try (HttpClient client = createNoRedirectClient(server)) {
            HttpResponse<String> signUpResponse = exchange(client, formPost("/user/signUp", Map.of(
                    "username", username,
                    "password", password,
                    "repeatPassword", password
            )));
            assertThat(signUpResponse.status().getCode()).isBetween(300, 399);

            String cookie = login(client, username, password);
            var owner = clinicServiceFixtures.requiredOwner("George", "Franklin");
            var pet = clinicServiceFixtures.requiredPet("George", "Franklin", "Leo");

            HttpResponse<String> response = exchange(client, HttpRequest.POST(
                    "/owners/" + owner.id() + "/pets/" + pet.id() + "/visits/new", Map.of(
                    "date", LocalDate.now().plusDays(1).toString(),
                    "description", "Follow-up",
                    "durationMinutes", 30,
                    "followUpPeriodMonths", 6
            )).contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.COOKIE, cookie));

            assertThat(response.status().getCode()).isBetween(300, 399);
            assertThat(response.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo("/owners/" + owner.id());
        }
    }

    private static Map<String, String> ownerForm(String firstName) {
        String telephone = String.valueOf(Math.abs(firstName.hashCode()));
        telephone = (telephone + "0000000000").substring(0, 10);
        return Map.of(
                "firstName", firstName,
                "lastName", "Tester",
                "address", "123 Test Street",
                "city", "Madison",
                "telephone", telephone
        );
    }
}
