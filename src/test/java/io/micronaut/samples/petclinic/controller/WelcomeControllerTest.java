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
 * Integration tests for {@link WelcomeController}.
 */
@MicronautTest
class WelcomeControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void shouldReturnWelcomePage() {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/"), String.class);
        
        assertThat((CharSequence) response.status()).isEqualTo(HttpStatus.OK);
        assertThat(response.body()).isNotNull();
    }
}
