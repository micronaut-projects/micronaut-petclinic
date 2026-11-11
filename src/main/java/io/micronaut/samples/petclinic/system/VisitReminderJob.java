package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.samples.petclinic.service.ClinicService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

/** Logs upcoming visit reminder candidates for the scheduled reminder demonstration. */
@Singleton
@Requires(property = "petclinic.visit-reminder.enabled", value = "true", defaultValue = "true")
public class VisitReminderJob {

    private static final Logger LOG = LoggerFactory.getLogger(VisitReminderJob.class);

    private final ClinicService clinicService;
    private final int lookaheadDays;

    /**
     * Creates the reminder job.
     *
     * @param clinicService service used to find upcoming visits
     * @param lookaheadDays the calendar days to look ahead
     */
    public VisitReminderJob(ClinicService clinicService,
                            @Value("${petclinic.visit-reminder.lookahead-days}") int lookaheadDays) {
        if (lookaheadDays < 0) {
            throw new IllegalArgumentException("Reminder lookahead days must not be negative");
        }
        this.clinicService = clinicService;
        this.lookaheadDays = lookaheadDays;
    }

    /** Logs each candidate; delivery and deduplication can be connected in Ticket 28. */
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
