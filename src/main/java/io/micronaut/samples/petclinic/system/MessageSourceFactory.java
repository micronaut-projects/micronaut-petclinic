package io.micronaut.samples.petclinic.system;

import io.micronaut.context.MessageSource;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.i18n.ResourceBundleMessageSource;
import jakarta.inject.Singleton;

/**
 * Factory for creating the MessageSource bean for internationalization.
 */
@Factory
public class MessageSourceFactory {

    /**
     * Creates a ResourceBundleMessageSource that loads messages from
     * i18n/messages*.properties files
     * 
     * @return The configured MessageSource
     */
    @Singleton
    public MessageSource createMessageSource() {
        return new ResourceBundleMessageSource("i18n.messages");
    }
}
