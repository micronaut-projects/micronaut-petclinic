package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Property;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@Property(name = "petclinic.notifications.enabled", value = "false")
class VisitNotificationsDisabledTest {

    @Inject
    ClinicService clinicService;

    @Inject
    NotificationOutbox notificationOutbox;

    @BeforeEach
    void setUp() {
        notificationOutbox.clear();
    }

    @Test
    void shouldNotCaptureNotificationsWhenDisabled() {
        Pet pet = clinicService.findPetById(1).orElseThrow();

        clinicService.saveVisit(new Visit(LocalDate.now(), "No notification expected", pet));

        assertThat(notificationOutbox.sentNotifications()).isEmpty();
    }
}
