package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.views.View;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for the home page.
 */
@Controller
public class WelcomeController {

    /**
     * Display the welcome/home page.
     * @return the welcome view
     */
    @Get("/")
    @View("welcome")
    public Map<String, Object> welcome() {
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Welcome to Micronaut Pet Clinic");
        return model;
    }
}
