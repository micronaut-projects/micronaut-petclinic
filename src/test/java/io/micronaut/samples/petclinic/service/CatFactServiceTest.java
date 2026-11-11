package io.micronaut.samples.petclinic.service;

import com.sun.net.httpserver.HttpServer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.samples.petclinic.client.CatFactClient;
import io.micronaut.samples.petclinic.client.OfflineCatFactClient;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CatFactServiceTest {

    @Test
    void defaultUrlPreservesHttpsScheme() {
        try (var context = ApplicationContext.run(Map.of(
                "petclinic.sample-data.enabled", false,
                "petclinic.cat-facts.enabled", false))) {
            assertThat(context.getProperty("micronaut.http.services.cat-facts.url", String.class))
                    .contains("https://catfact.ninja");
        }
    }

    @Test
    void disabledIntegrationUsesOfflineBean() {
        try (var context = ApplicationContext.run(Map.of("petclinic.cat-facts.enabled", false))) {
            assertThat(context.getBean(CatFactClient.class)).isInstanceOf(OfflineCatFactClient.class);
            assertThat(context.getBean(CatFactService.class).findFact())
                    .contains("Cats use their whiskers to sense nearby objects.");
        }
    }

    @Test
    void declarativeClientDecodesLocalResponsesAndToleratesFailures() throws Exception {
        var status = new AtomicInteger(200);
        var body = new AtomicReference<>("{\"fact\":\"A local cat fact.\",\"length\":17}");
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fact", exchange -> {
            requests.incrementAndGet();
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
        try (var context = ApplicationContext.run(Map.of(
                "petclinic.cat-facts.enabled", true,
                "micronaut.http.services.cat-facts.url", "http://127.0.0.1:" + server.getAddress().getPort(),
                "micronaut.http.services.cat-facts.connect-timeout", "1s",
                "micronaut.http.services.cat-facts.read-timeout", "1s"))) {
            var service = context.getBean(CatFactService.class);
            assertThat(context.getBean(CatFactClient.class)).isNotInstanceOf(OfflineCatFactClient.class);
            assertThat(service.findFact()).contains("A local cat fact.");
            status.set(503);
            assertThat(service.findFact()).isEmpty();
            status.set(200);
            body.set("invalid json");
            assertThat(service.findFact()).isEmpty();
            body.set("{\"fact\":\" \"}");
            assertThat(service.findFact()).isEmpty();
            assertThat(requests.get()).isEqualTo(4);
            server.stop(0);
            assertThat(service.findFact()).isEmpty();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void timeoutAndEmptyResponsesAreOptional() {
        assertThat(new CatFactService(() -> {
            throw io.micronaut.http.client.exceptions.ReadTimeoutException.TIMEOUT_EXCEPTION;
        }).findFact()).isEmpty();
        assertThat(new CatFactService(() -> null).findFact()).isEmpty();
        assertThat(new CatFactService(() -> new io.micronaut.samples.petclinic.dto.CatFact(null))
                .findFact()).isEmpty();
    }
}
