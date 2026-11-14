package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/**
 * Periodically logs upcoming visits as reminder candidates.
 *
 * <p>The {@code petclinic.visit-reminder.enabled} property controls whether this
 * bean is created. Reminders are enabled by default and disabled in the default
 * test configuration. The {@code petclinic.visit-reminder.schedule} property
 * supplies the cron expression, and {@code petclinic.visit-reminder.lookahead-days}
 * sets the number of calendar days to look ahead.
 *
 * <p>Each run includes today through the lookahead date, using the server's local
 * time zone. Candidates are logged on every run without sending email or recording
 * delivery state.
 */
@Singleton
@Requires(property = "petclinic.visit-reminder.enabled", value = "true", defaultValue = "true")
public class VisitReminderJob {

    private static final Logger LOG = LoggerFactory.getLogger(VisitReminderJob.class);

    private final ClinicService clinicService;
    private final int lookaheadDays;

    /**
     * Creates a reminder job with the configured calendar date window.
     *
     * @param clinicService service used to find upcoming visits
     * @param lookaheadDays the nonnegative number of calendar days after today to
     *                      include; zero includes only today's visits
     * @throws IllegalArgumentException if {@code lookaheadDays} is negative
     */
    public VisitReminderJob(ClinicService clinicService,
                            @Value("${petclinic.visit-reminder.lookahead-days}") int lookaheadDays) {
        if (lookaheadDays < 0) {
            throw new IllegalArgumentException("Reminder lookahead days must not be negative");
        }
        this.clinicService = clinicService;
        this.lookaheadDays = lookaheadDays;
    }

    /**
     * Queries the current reminder window and logs its candidate count and each
     * visit's identifier, pet details, and date.
     *
     * <p>Invoked by Micronaut according to the configured cron expression. Visits
     * remain eligible on subsequent runs while their dates fall within the window.
     */
    @Scheduled(cron = "${petclinic.visit-reminder.schedule}")
    void remindUpcomingVisits() {
        var visits = clinicService.findUpcomingVisits(LocalDate.now(), lookaheadDays);
        LOG.info("Found {} visit reminder candidates", visits.size());
        for (var visit : visits) {
            LOG.info("Visit reminder candidate: visitId={}, petId={}, pet={}, date={}",
                    visit.id(), visit.pet().id(), visit.pet().name(), visit.date());
        }
    }
}
