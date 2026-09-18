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
@Property(name = "petclinic.notifications.enabled", value = "true")
@Property(name = "petclinic.notifications.provider", value = "fake")
@Property(name = "petclinic.notifications.reminders.enabled", value = "true")
class VisitNotificationsEnabledTest {

    @Inject
    ClinicService clinicService;

    @Inject
    VisitReminderScheduler visitReminderScheduler;

    @Inject
    NotificationOutbox notificationOutbox;

    @BeforeEach
    void setUp() {
        notificationOutbox.clear();
    }

    @Test
    void shouldCaptureVisitConfirmationWhenVisitIsCreated() {
        Pet pet = clinicService.findPetById(1).orElseThrow();

        Visit savedVisit = clinicService.saveVisit(new Visit(LocalDate.now(), "Dental cleaning", pet));

        assertThat(savedVisit.id()).isNotNull();
        assertThat(notificationOutbox.sentNotifications())
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.subject()).contains("Visit confirmed");
                    assertThat(notification.body()).contains("Dental cleaning");
                    assertThat(notification.body()).contains(pet.name());
                });
    }

    @Test
    void shouldReuseNotificationServiceForScheduledReminders() {
        Pet pet = clinicService.findPetById(1).orElseThrow();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        clinicService.saveVisit(new Visit(tomorrow, "Reminder candidate", pet));
        notificationOutbox.clear();

        int sent = visitReminderScheduler.sendRemindersForDate(tomorrow);

        assertThat(sent).isEqualTo(1);
        assertThat(notificationOutbox.sentNotifications())
                .singleElement()
                .satisfies(notification -> {
                    assertThat(notification.subject()).contains("Visit reminder");
                    assertThat(notification.body()).contains("Reminder candidate");
                    assertThat(notification.body()).contains(tomorrow.toString());
                });
    }
}
