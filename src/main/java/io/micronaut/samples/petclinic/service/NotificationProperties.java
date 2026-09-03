package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configuration properties for visit notification delivery.
 */
@ConfigurationProperties("petclinic.notifications")
public class NotificationProperties {

    private boolean enabled;
    private String provider = "fake";
    private String fromAddress = "noreply@petclinic.local";
    private String fromName = "Micronaut PetClinic";
    private String recipient = "notifications@petclinic.local";
    private final ReminderProperties reminders = new ReminderProperties();

    /**
     * @return whether notifications are globally enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled whether notifications are globally enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return provider identifier, for example {@code fake} or {@code smtp}
     */
    public String getProvider() {
        return provider;
    }

    /**
     * @param provider provider identifier, for example {@code fake} or {@code smtp}
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * @return sender email address used for SMTP delivery
     */
    public String getFromAddress() {
        return fromAddress;
    }

    /**
     * @param fromAddress sender email address used for SMTP delivery
     */
    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    /**
     * @return sender display name used for SMTP delivery
     */
    public String getFromName() {
        return fromName;
    }

    /**
     * @param fromName sender display name used for SMTP delivery
     */
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    /**
     * @return mailbox that receives demo notifications
     */
    public String getRecipient() {
        return recipient;
    }

    /**
     * @param recipient mailbox that receives demo notifications
     */
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    /**
     * @return reminder scheduler configuration
     */
    public ReminderProperties getReminders() {
        return reminders;
    }

    /**
     * Scheduler-specific properties for upcoming-visit reminders.
     */
    @ConfigurationProperties("reminders")
    public static class ReminderProperties {
        private boolean enabled;
        private int daysAhead = 1;
        private String fixedDelay = "1h";
        private String initialDelay = "10s";

        /**
         * @return whether scheduled reminders are enabled
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * @param enabled whether scheduled reminders are enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * @return number of days ahead to look for visits when sending reminders
         */
        public int getDaysAhead() {
            return daysAhead;
        }

        /**
         * @param daysAhead number of days ahead to look for visits when sending reminders
         */
        public void setDaysAhead(int daysAhead) {
            this.daysAhead = daysAhead;
        }

        /**
         * @return scheduler fixed-delay expression
         */
        public String getFixedDelay() {
            return fixedDelay;
        }

        /**
         * @param fixedDelay scheduler fixed-delay expression
         */
        public void setFixedDelay(String fixedDelay) {
            this.fixedDelay = fixedDelay;
        }

        /**
         * @return scheduler initial-delay expression
         */
        public String getInitialDelay() {
            return initialDelay;
        }

        /**
         * @param initialDelay scheduler initial-delay expression
         */
        public void setInitialDelay(String initialDelay) {
            this.initialDelay = initialDelay;
        }
    }
}
