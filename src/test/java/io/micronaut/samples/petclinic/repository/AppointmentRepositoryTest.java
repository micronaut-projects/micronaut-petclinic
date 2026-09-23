package io.micronaut.samples.petclinic.repository;

import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static io.micronaut.samples.petclinic.model.Appointment.Status.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the shared repository contract using the Oracle implementation. */
@MicronautTest(environments = "oracle")
class AppointmentRepositoryTest {
    @Inject AppointmentRepository appointments;

    @Test
    void findsOnlyDemoAppointmentsInDisplayOrder() {
        appointments.save(new Appointment("Ordinary appointment", 0));

        var demo = appointments.findDemoAppointments();
        assertThat(demo).extracting(Appointment::demoKey)
                .containsExactly("PRIORITY_CURRENT", "PRIORITY_FALLBACK");
        assertThat(demo).extracting(Appointment::displayOrder).containsExactly(1, 2);
    }

    @Test
    void savingAnExistingAppointmentUpdatesItWithoutInsertingAnotherRow() {
        var original = appointments.save(new Appointment("Save regression appointment", 99));
        long count = appointments.count();

        var updated = appointments.findById(original.id()).orElseThrow();
        for (var status : new Appointment.Status[]{HELD_BY_REGULAR, BOOKED_FOR_REGULAR}) {
            updated = appointments.save(updated.withStatus(status));
            assertThat(updated).isEqualTo(original.withStatus(status));
            assertThat(appointments.findById(original.id())).contains(updated);
            assertThat(appointments.count()).isEqualTo(count);
        }
        assertThat(original.status()).isEqualTo(AVAILABLE);
    }

    @Test
    void locksAndResetsOnlyDemoAppointments() {
        var ordinary = appointments.save(new Appointment(null, "Ordinary appointment", 0,
                BOOKED_FOR_REGULAR, null));
        var demo = appointments.lockDemoAppointments();
        assertThat(demo).extracting(Appointment::demoKey)
                .containsExactly("PRIORITY_CURRENT", "PRIORITY_FALLBACK");
        for (var appointment : demo) {
            appointments.save(appointment.withStatus(BOOKED_FOR_EMERGENCY));
        }

        for (var appointment : appointments.lockDemoAppointments()) {
            appointments.save(appointment.withStatus(AVAILABLE));
        }
        assertThat(appointments.findDemoAppointments()).allMatch(a -> a.status() == AVAILABLE);
        assertThat(appointments.findById(ordinary.id()).orElseThrow().status()).isEqualTo(BOOKED_FOR_REGULAR);
    }
}
