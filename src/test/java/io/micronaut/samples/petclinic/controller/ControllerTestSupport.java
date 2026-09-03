package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static HttpClient createNoRedirectClient(EmbeddedServer server) {
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
        configuration.setFollowRedirects(false);
        configuration.setExceptionOnErrorStatus(false);
        return HttpClient.create(server.getURL(), configuration);
    }

    static MutableHttpRequest<?> formPost(String uri, Map<String, String> form) {
        return HttpRequest.POST(uri, form)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);
    }

    @SuppressWarnings("unchecked")
    static HttpResponse<String> exchange(HttpClient client, HttpRequest<?> request) {
        try {
            return client.toBlocking().exchange(request, String.class);
        } catch (HttpClientResponseException e) {
            return (HttpResponse<String>) e.getResponse();
        }
    }

    static HttpResponse<String> loginResponse(HttpClient client, String username, String password) {
        return exchange(client, formPost("/login", Map.of(
                "username", username,
                "password", password
        )));
    }

    static String login(HttpClient client, String username, String password) {
        HttpResponse<String> response = loginResponse(client, username, password);
        assertThat(response.status().getCode()).isBetween(300, 399);
        assertThat(response.getHeaders().get(HttpHeaders.LOCATION)).isEqualTo("/");
        return firstCookie(response);
    }

    private static String firstCookie(HttpResponse<?> response) {
        List<String> setCookies = response.getHeaders().getAll(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotEmpty();
        return setCookies.getFirst().split(";", 2)[0];
    }
}
