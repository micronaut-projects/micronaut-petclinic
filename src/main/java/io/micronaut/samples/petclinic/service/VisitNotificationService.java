package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.model.EmailNotification;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Visit;
import jakarta.inject.Singleton;

import java.util.HashSet;

/**
 * Builds visit-related email notifications and delegates delivery to {@link EmailService}.
 */
@Singleton
public class VisitNotificationService {

    private final EmailService emailService;
    private final NotificationProperties notificationProperties;

    /**
     * @param emailService service responsible for notification delivery
     * @param notificationProperties notification configuration
     */
    public VisitNotificationService(EmailService emailService,
                                    NotificationProperties notificationProperties) {
        this.emailService = emailService;
        this.notificationProperties = notificationProperties;
    }

    /**
     * Sends a confirmation email for a newly created visit.
     *
     * @param visit created visit
     * @return {@code true} when the notification was sent or captured
     */
    public boolean sendVisitConfirmation(Visit visit) {
        return emailService.send(new EmailNotification(
                notificationProperties.getRecipient(),
                new HashSet<>(),
                "Visit confirmed for " + petName(visit),
                """
                Visit confirmation

                Pet: %s
                Owner: %s
                Date: %s
                Description: %s
                """.formatted(
                        petName(visit),
                        ownerName(visit),
                        visit.date(),
                        visit.description()
                ).trim()
        ));
    }

    /**
     * Sends a reminder email for an upcoming visit.
     *
     * @param visit upcoming visit
     * @return {@code true} when the notification was sent or captured
     */
    public boolean sendVisitReminder(Visit visit) {
        return emailService.send(new EmailNotification(
                notificationProperties.getRecipient(),
                new HashSet<>(),
                "Visit reminder for " + petName(visit),
                """
                Scheduled visit reminder

                Pet: %s
                Owner: %s
                Date: %s
                Description: %s
                """.formatted(
                        petName(visit),
                        ownerName(visit),
                        visit.date(),
                        visit.description()
                ).trim()
        ));
    }

    /**
     * Resolves the visit pet name or a fallback label when unavailable.
     *
     * @param visit visit to inspect
     * @return pet display name
     */
    private static String petName(Visit visit) {
        Pet pet = visit.pet();
        return pet != null && pet.name() != null ? pet.name() : "Unknown pet";
    }

    /**
     * Resolves the visit owner name or a fallback label when unavailable.
     *
     * @param visit visit to inspect
     * @return owner display name
     */
    private static String ownerName(Visit visit) {
        Pet pet = visit.pet();
        Owner owner = pet != null ? pet.getOwner() : null;
        if (owner == null) {
            return "Unknown owner";
        }
        return owner.firstName() + " " + owner.lastName();
    }
}
