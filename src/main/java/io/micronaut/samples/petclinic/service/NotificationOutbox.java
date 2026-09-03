package io.micronaut.samples.petclinic.service;

import io.micronaut.samples.petclinic.model.EmailNotification;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory store used by the fake notification provider during tests and local development.
 */
@Singleton
public class NotificationOutbox {

    private final List<EmailNotification> notifications = new ArrayList<>();

    /**
     * Records a notification as if it had been sent.
     *
     * @param notification notification payload to capture
     */
    public synchronized void record(EmailNotification notification) {
        notifications.add(notification);
    }

    /**
     * Returns a snapshot of captured notifications.
     *
     * @return immutable copy of recorded notifications
     */
    public synchronized List<EmailNotification> sentNotifications() {
        return List.copyOf(notifications);
    }

    /**
     * Clears all captured notifications.
     */
    public synchronized void clear() {
        notifications.clear();
    }
}
