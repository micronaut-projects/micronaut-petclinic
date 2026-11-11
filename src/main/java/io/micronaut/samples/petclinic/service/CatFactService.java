package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.client.CatFactClient;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/** Keeps optional external content from interrupting clinic workflows. */
@Singleton
public class CatFactService {

    private static final Logger LOG = LoggerFactory.getLogger(CatFactService.class);
    private final CatFactClient client;

    public CatFactService(CatFactClient client) {
        this.client = client;
    }

    public Optional<String> findFact() {
        try {
            var response = client.fact();
            if (response == null || response.fact() == null || response.fact().isBlank()) {
                return Optional.empty();
            }
            return Optional.of(response.fact());
        } catch (RuntimeException e) {
            // HTTP errors, timeouts and decoding failures must not break owner details.
            LOG.debug("Optional cat fact lookup failed", e);
            return Optional.empty();
        }
    }
}
