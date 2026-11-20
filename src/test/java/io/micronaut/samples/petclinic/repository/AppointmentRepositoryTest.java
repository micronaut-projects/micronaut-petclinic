package io.micronaut.samples.petclinic.repository;

import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.micronaut.samples.petclinic.model.Appointment.Status.AVAILABLE;
import static io.micronaut.samples.petclinic.model.Appointment.Status.BOOKED_FOR_EMERGENCY;
import static io.micronaut.samples.petclinic.model.Appointment.Status.BOOKED_FOR_REGULAR;
import static io.micronaut.samples.petclinic.model.Appointment.Status.HELD_BY_REGULAR;
import static org.assertj.core.api.Assertions.assertThat;

/** Exercises appointment queries on the active database profile, including locking-query syntax. */
@MicronautTest
class AppointmentRepositoryTest {
    @Inject AppointmentRepository appointments;

    @Test
    void findsAvailableAppointmentsInDisplayOrder() {
        var later = appointments.save(new Appointment("Later appointment", 100));
        var earlier = appointments.save(new Appointment("Earlier appointment", 99));
        var booked = appointments.save(new Appointment(null, "Booked appointment", 98, BOOKED_FOR_REGULAR));

        var available = appointments.findAvailableAppointments();
        assertThat(available).allMatch(a -> a.status() == AVAILABLE);
        assertThat(available).extracting(Appointment::displayOrder).isSorted();
        assertThat(available).extracting(Appointment::id)
                .containsSubsequence(earlier.id(), later.id()).doesNotContain(booked.id());
    }

    @Test
    void savingAnExistingAppointmentUpdatesItWithoutInsertingAnotherRow() {
        var original = appointments.save(new Appointment("Save regression appointment", 99));
        long count = appointments.count();

        var updated = appointments.findByIdForUpdate(original.id()).orElseThrow();
        for (var status : new Appointment.Status[]{HELD_BY_REGULAR, BOOKED_FOR_REGULAR}) {
            updated = appointments.save(updated.withStatus(status));
            assertThat(updated).isEqualTo(original.withStatus(status));
            assertThat(appointments.findById(original.id())).contains(updated);
            assertThat(appointments.count()).isEqualTo(count);
        }
        assertThat(original.status()).isEqualTo(AVAILABLE);
    }

    @Test
    void bookedAppointmentRemainsFindableButIsNoLongerAvailable() {
        var appointment = appointments.save(new Appointment("Appointment to book", 99));
        var booked = appointments.save(appointment.withStatus(BOOKED_FOR_EMERGENCY));

        assertThat(appointments.existsById(booked.id())).isTrue();
        assertThat(appointments.findById(booked.id())).contains(booked);
        assertThat(appointments.findAvailableAppointments()).extracting(Appointment::id).doesNotContain(booked.id());
    }

    @Test
    void scenariosSeparateAvailabilityAndResetCandidates() {
        var priority = appointments.save(new Appointment("Priority fixture", 99));
        var sessionless = appointments.save(new Appointment("Sessionless fixture", 99, Appointment.Scenario.SESSIONLESS));
        var confirmed = appointments.save(new Appointment("Confirmed fixture", 100, Appointment.Scenario.SESSIONLESS)
                .withStatus(Appointment.Status.BOOKED_AFTER_CONFIRMATION));

        assertThat(appointments.findAvailableAppointments()).contains(priority).doesNotContain(sessionless, confirmed);
        assertThat(appointments.findAvailableAppointments(Appointment.Scenario.SESSIONLESS))
                .contains(sessionless).doesNotContain(priority, confirmed);
        assertThat(appointments.findByIdAndScenario(sessionless.id(), Appointment.Scenario.SESSIONLESS)).contains(sessionless);
        assertThat(appointments.findByIdAndScenario(sessionless.id(), Appointment.Scenario.PRIORITY)).isEmpty();
        assertThat(appointments.findByIdAndScenarioForUpdate(sessionless.id(), Appointment.Scenario.SESSIONLESS)).contains(sessionless);
        assertThat(appointments.findByIdAndScenarioForUpdate(sessionless.id(), Appointment.Scenario.PRIORITY)).isEmpty();
        assertThat(appointments.findByScenarioOrderByDisplayOrderAscAndIdAsc(Appointment.Scenario.SESSIONLESS))
                .contains(sessionless, confirmed).doesNotContain(priority)
                .extracting(Appointment::displayOrder).isSorted();
        assertThat(appointments.findByScenarioAndStatusOrderByDisplayOrderAscAndIdAsc(
                Appointment.Scenario.SESSIONLESS, Appointment.Status.BOOKED_AFTER_CONFIRMATION)).contains(confirmed)
                .doesNotContain(priority, sessionless);
    }
}
