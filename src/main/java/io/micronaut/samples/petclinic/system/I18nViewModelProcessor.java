package io.micronaut.samples.petclinic.system;

import io.micronaut.context.MessageSource;
import io.micronaut.context.env.Environment;
import io.micronaut.http.HttpRequest;
import io.micronaut.views.ModelAndView;
import io.micronaut.views.model.ViewModelProcessor;
import jakarta.inject.Singleton;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enrich every view model with i18n helpers.
 */
@Singleton
public class I18nViewModelProcessor implements ViewModelProcessor<Object, HttpRequest<?>> {

    private final MessageSource messageSource;
    private final io.micronaut.http.server.util.locale.HttpLocaleResolver httpLocaleResolver;
    private final Environment environment;

    /**
     * Creates the processor with message and locale resolution dependencies.
     *
     * @param messageSource the application message source
     * @param httpLocaleResolver the resolver used to select the request locale
     */
    public I18nViewModelProcessor(MessageSource messageSource,
                                  io.micronaut.http.server.util.locale.HttpLocaleResolver httpLocaleResolver,
                                  Environment environment) {
        this.messageSource = messageSource;
        this.httpLocaleResolver = httpLocaleResolver;
        this.environment = environment;
    }


    /**
     * Adds locale, message, and request helpers to the view model.
     *
     * @param request the current HTTP request
     * @param modelAndView the model and view being rendered
     */
    @Override
    public void process(@NonNull HttpRequest<?> request, @NonNull ModelAndView<Object> modelAndView) {
        Locale locale = httpLocaleResolver.resolveOrDefault(request);

        Map<String, Object> model = new HashMap<>(modelAndView.getModel()
                .filter(Map.class::isInstance)
                .map(m -> (Map<String, Object>) m)
                .orElseGet(Map::of));

        model.putIfAbsent("locale", locale);
        model.putIfAbsent("msg", new Msg(messageSource, locale));
        model.putIfAbsent("request", request);
        model.putIfAbsent("oracleProfile", environment.getActiveNames().contains("oracle"));

        modelAndView.setModel(model);
    }

    /**
     * Small wrapper so templates can do: ${msg.text("nav.home")}.
     */
    public static final class Msg {
        private final MessageSource messageSource;
        private final Locale locale;

        Msg(MessageSource messageSource, Locale locale) {
            this.messageSource = messageSource;
            this.locale = locale;
        }

        /**
         * Resolves a message code for the current locale.
         *
         * @param code the message code
         * @return the localized message, or the code when no message exists
         */
        public String text(String code) {
            return messageSource.getMessage(code, locale).orElse(code);
        }

        /**
         * Resolves a parameterized message code for the current locale.
         *
         * @param code the message code
         * @param args message arguments
         * @return the localized message, or the code when no message exists
         */
        public String text(String code, Object... args) {
            return messageSource.getMessage(code, locale, args).orElse(code);
        }
    }
}
