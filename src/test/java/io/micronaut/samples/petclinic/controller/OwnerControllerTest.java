package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
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
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/owners/list?lastName=Davis"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Davis");
    }

    @Test
    void shouldShowOwnerDetails() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/owners/1"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("George");
        assertThat(response.body()).contains("Franklin");
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
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/owners/1/edit"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).contains("Edit Owner");
        assertThat(response.body()).contains("George");
    }
}
