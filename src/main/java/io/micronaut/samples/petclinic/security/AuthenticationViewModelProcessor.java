package io.micronaut.samples.petclinic.security;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.utils.SecurityService;
import io.micronaut.views.ModelAndView;
import io.micronaut.views.model.ViewModelProcessor;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposes authentication state to rendered views.
 */
@Singleton
public class AuthenticationViewModelProcessor implements ViewModelProcessor<Object, HttpRequest<?>> {

    private static final String AUTHENTICATED = "authenticated";

    private final SecurityService securityService;

    /**
     * Creates the processor with the security service used to inspect the current request.
     *
     * @param securityService service used to expose the current authentication state
     */
    public AuthenticationViewModelProcessor(SecurityService securityService) {
        this.securityService = securityService;
    }

    /**
     * Adds the current authentication state to the request and view model.
     *
     * @param request the current HTTP request
     * @param modelAndView the model and view being rendered
     */
    @Override
    public void process(@NonNull HttpRequest<?> request, @NonNull ModelAndView<Object> modelAndView) {
        boolean authenticated = securityService.isAuthenticated();
        request.setAttribute(AUTHENTICATED, authenticated);

        Map<String, Object> model = new HashMap<>(modelAndView.getModel()
                .filter(Map.class::isInstance)
                .map(m -> (Map<String, Object>) m)
                .orElseGet(Map::of));

        model.putIfAbsent(AUTHENTICATED, authenticated);
        modelAndView.setModel(model);
    }
}
