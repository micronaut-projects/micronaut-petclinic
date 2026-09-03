package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.View;

import java.util.Map;

/**
 * Controller for the home page.
 */
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller
public class WelcomeController {

    /**
     * Creates the welcome controller.
     */
    public WelcomeController() {
    }

    /**
     * Display the welcome/home page.
     * @return the welcome view
     */
    @Get("/")
    @View("welcome")
    public Map<String, Object> welcome() {
        return Map.of("title", "Welcome to Micronaut Pet Clinic");
    }
}
