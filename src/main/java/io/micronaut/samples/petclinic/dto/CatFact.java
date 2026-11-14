package io.micronaut.samples.petclinic.dto;

import io.micronaut.serde.annotation.Serdeable;

/** Optional educational content returned by the cat facts service. */
@Serdeable
public record CatFact(String fact) {
}
