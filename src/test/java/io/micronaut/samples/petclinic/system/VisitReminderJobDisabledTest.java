package io.micronaut.samples.petclinic.system;

import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the default test profile disables background reminders. */
@MicronautTest
class VisitReminderJobDisabledTest {

    @Inject
    ApplicationContext context;

    @Test
    void shouldDisableRemindersByDefaultInTests() {
        assertThat(context.containsBean(VisitReminderJob.class)).isFalse();
    }
}
