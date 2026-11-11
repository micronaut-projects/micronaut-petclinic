package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.ReadTimeoutException;
import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.samples.petclinic.service.CatFactService;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** External content failures must not prevent rendering clinic data. */
@MicronautTest
class CatFactFailureTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ClinicServiceFixtures fixtures;

    @MockBean(CatFactService.class)
    CatFactService failingFacts() {
        return new CatFactService(() -> {
            throw ReadTimeoutException.TIMEOUT_EXCEPTION;
        });
    }

    @Test
    void ownerAndPetDetailsRemainAvailableWhenFactsTimeOut() {
        var owner = fixtures.requiredOwner("George", "Franklin");
        String page = client.toBlocking().retrieve("/owners/" + owner.id());
        assertThat(page).contains("George", "Franklin", "Leo");
        assertThat(page).doesNotContain("id=\"cat-fact\"");
    }
}
