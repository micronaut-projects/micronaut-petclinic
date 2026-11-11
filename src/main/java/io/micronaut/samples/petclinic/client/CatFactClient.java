package io.micronaut.samples.petclinic.client;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.samples.petclinic.dto.CatFact;

/** Declarative client for the Cat Facts API (or a compatible local service). */
@Client("cat-facts")
@Requires(property = "petclinic.cat-facts.enabled", value = "true")
public interface CatFactClient {

    @Get("/fact")
    CatFact fact();
}
