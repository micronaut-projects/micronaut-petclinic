package io.micronaut.samples.petclinic.client;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.dto.CatFact;
import jakarta.inject.Singleton;

/** Deterministic replacement for offline runs; never makes an HTTP request. */
@Singleton
@Requires(property = "petclinic.cat-facts.enabled", notEquals = "true", defaultValue = "false")
public class OfflineCatFactClient implements CatFactClient {

    @Override
    public CatFact fact() {
        return new CatFact("Cats use their whiskers to sense nearby objects.");
    }
}
