package io.micronaut.samples.petclinic.model;

import java.util.Set;

/**
 * Application-level email payload used by notification flows.
 *
 * @param to recipient email address
 * @param cc optional carbon-copy recipient addresses
 * @param subject email subject
 * @param body plain-text body
 */
public record EmailNotification(
        String to,
        Set<String> cc,
        String subject,
        String body) {
}
