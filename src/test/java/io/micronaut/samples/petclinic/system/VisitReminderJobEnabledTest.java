package io.micronaut.samples.petclinic.system;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies tests can explicitly enable the scheduled reminder bean. */
@MicronautTest
@Property(name = "petclinic.visit-reminder.enabled", value = "true")
@Property(name = "petclinic.visit-reminder.schedule", value = "0 0 0 1 1 *")
@Property(name = "petclinic.visit-reminder.lookahead-days", value = "3")
class VisitReminderJobEnabledTest {

    @Inject
    ApplicationContext context;

    @Test
    void shouldCreateReminderJobWhenEnabled() {
        assertThat(context.containsBean(VisitReminderJob.class)).isTrue();
        context.getBean(VisitReminderJob.class).remindUpcomingVisits();
    }
}
