package io.micronaut.samples.petclinic.service;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.time.LocalDate;

/**
 * Periodically sends reminders for upcoming visits.
 */
@Singleton
public class VisitReminderScheduler {

    private final ClinicService clinicService;
    private final VisitNotificationService visitNotificationService;
    private final NotificationProperties notificationProperties;

    /**
     * @param clinicService service used to query scheduled visits
     * @param visitNotificationService service used to send reminder notifications
     * @param notificationProperties notification configuration
     */
    public VisitReminderScheduler(ClinicService clinicService,
                                  VisitNotificationService visitNotificationService,
                                  NotificationProperties notificationProperties) {
        this.clinicService = clinicService;
        this.visitNotificationService = visitNotificationService;
        this.notificationProperties = notificationProperties;
    }

    @Scheduled(
            fixedDelay = "${petclinic.notifications.reminders.fixed-delay:1h}",
            initialDelay = "${petclinic.notifications.reminders.initial-delay:10s}"
    )
    /**
     * Scheduled entry point that sends reminders for the configured future date window.
     */
    public void sendScheduledReminders() {
        if (!notificationProperties.getReminders().isEnabled()) {
            return;
        }
        sendRemindersForDate(LocalDate.now().plusDays(notificationProperties.getReminders().getDaysAhead()));
    }

    /**
     * Sends reminders for every visit scheduled on the supplied date.
     *
     * @param targetDate date to inspect
     * @return number of reminders successfully sent or captured
     */
    public int sendRemindersForDate(LocalDate targetDate) {
        if (!notificationProperties.isEnabled()) {
            return 0;
        }
        int sent = 0;
        for (var visit : clinicService.findVisitsByDate(targetDate)) {
            if (visitNotificationService.sendVisitReminder(visit)) {
                sent++;
            }
        }
        return sent;
    }
}
