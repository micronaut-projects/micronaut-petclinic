package io.micronaut.samples.petclinic.service;

import io.micronaut.email.BodyType;
import io.micronaut.email.Email;
import io.micronaut.email.EmailSender;
import io.micronaut.samples.petclinic.model.EmailNotification;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link EmailService} implementation.
 *
 * <p>When the configured provider is {@code smtp}, this bean delegates to Micronaut Email's
 * {@link EmailSender}. Otherwise it records messages in the in-memory
 * {@link NotificationOutbox} so notification flows can run without external infrastructure.</p>
 */
@Singleton
public class DefaultEmailService implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultEmailService.class);

    private final NotificationProperties notificationProperties;
    private final NotificationOutbox notificationOutbox;
    private final EmailSender<?, ?> emailSender;

    /**
     * Creates an email service that can either send SMTP email or capture notifications locally.
     *
     * @param notificationProperties notification configuration
     * @param notificationOutbox in-memory outbox used by the fake provider
     * @param emailSender Micronaut Email sender used for SMTP delivery
     */
    public DefaultEmailService(NotificationProperties notificationProperties,
                               NotificationOutbox notificationOutbox,
                               EmailSender<?, ?> emailSender) {
        this.notificationProperties = notificationProperties;
        this.notificationOutbox = notificationOutbox;
        this.emailSender = emailSender;
    }

    /**
     * Sends a notification using the configured provider.
     *
     * @param notification plain-text email payload
     * @return {@code true} when sent or captured, {@code false} when notifications are disabled
     */
    @Override
    public boolean send(EmailNotification notification) {
        if (!notificationProperties.isEnabled()) {
            LOG.debug("Notifications disabled; skipping subject='{}'", notification.subject());
            return false;
        }

        if (usesSmtp()) {
            Email.Builder emailBuilder = Email.builder()
                    .from(notificationProperties.getFromAddress())
                    .to(notification.to())
                    .subject(notification.subject())
                    .body(notification.body(), BodyType.TEXT);

            emailSender.send(emailBuilder);
            return true;
        }

        notificationOutbox.record(notification);
        LOG.info("Captured fake email to={} subject={}", notification.to(), notification.subject());
        return true;
    }

    /**
     * @return {@code true} when SMTP delivery is configured
     */
    private boolean usesSmtp() {
        return "smtp".equalsIgnoreCase(notificationProperties.getProvider());
    }
}
