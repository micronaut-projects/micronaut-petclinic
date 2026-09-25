package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.model.EmailNotification;

/**
 * Sends application-level email notifications.
 */
public interface EmailService {

    /**
     * Sends or captures the supplied notification depending on the active provider configuration.
     *
     * @param notification plain-text email payload to process
     * @return {@code true} when the notification was accepted for delivery or capture;
     * {@code false} when notifications are disabled
     */
    boolean send(EmailNotification notification);
}
