package io.micronaut.samples.petclinic.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Consumes;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.samples.petclinic.dto.SignUpForm;
import io.micronaut.samples.petclinic.execption.UserAlreadyExistsException;
import io.micronaut.samples.petclinic.service.RegisterService;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.views.ModelAndView;
import io.micronaut.views.View;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.net.URI;
import java.util.Map;

/**
 * Handles login and sign-up pages for session-based authentication.
 */
@Secured(SecurityRule.IS_ANONYMOUS)
@Controller(UserController.PATH)
public class UserController {

    public static final String PATH = "/user";
    private static final String PATH_AUTH = "/auth";
    private static final String PATH_AUTH_FAILED = "/authFailed";
    private static final String PATH_SIGN_UP = "/signUp";
    private static final String PATH_SIGNUP = "/user/signUp";
    private static final String VIEW_SIGNUP = "/user/signup";
    private static final String VIEW_AUTH = "/user/auth";
    private final RegisterService registerService;
    private final URI uriAuth;

    /**
     * Creates the user controller.
     *
     * @param registerService service used to create new users
     */
    public UserController(RegisterService registerService) {
        this.registerService = registerService;
        this.uriAuth = UriBuilder.of(PATH).path(PATH_AUTH).build();
    }

    /**
     * Renders the login page.
     *
     * @return an empty model for the login view
     */
    @Produces(MediaType.TEXT_HTML)
    @Get(PATH_AUTH)
    @View(VIEW_AUTH)
    public Map<String, Object> auth() {
        return Map.of();
    }

    /**
     * Renders the login page after Micronaut Security reports a failed login.
     *
     * @return an empty model for the login view
     */
    @Produces(MediaType.TEXT_HTML)
    @Get(PATH_AUTH_FAILED)
    @View(VIEW_AUTH)
    public Map<String, Object> authFailed() {
        return Map.of();
    }

    /**
     * Processes the sign-up form.
     *
     * @param signUpForm the submitted sign-up form
     * @return a redirect to the login page, or 422 when the user already exists
     */
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Post(PATH_SIGN_UP)
    public HttpResponse<?> signUpSave(@NotNull @Valid @Body SignUpForm signUpForm) {
        try {
            registerService.register(signUpForm.username(), signUpForm.password());
        } catch (UserAlreadyExistsException e) {
            return HttpResponse.unprocessableEntity();
        }
        return HttpResponse.seeOther(uriAuth);
    }

    /**
     * Renders the sign-up page.
     *
     * @return an empty model for the sign-up view
     */
    @Produces(MediaType.TEXT_HTML)
    @Get(PATH_SIGN_UP)
    @View(VIEW_SIGNUP)
    public Map<String, Object> signUp() {
        return Map.of();
    }

    /**
     * Renders the sign-up form again when validation fails.
     *
     * @param request the request containing the submitted form
     * @param ex the validation exception
     * @return the sign-up form view for sign-up validation errors
     */
    @Error(exception = ConstraintViolationException.class)
    public HttpResponse<?> onConstraintViolationException(HttpRequest<?> request, ConstraintViolationException ex) {
        if (request.getPath().equals(PATH_SIGNUP)) {
            return request.getBody(SignUpForm.class)
                    .map(signUpForm -> HttpResponse.ok()
                            .body(new ModelAndView<>(VIEW_SIGNUP, Map.of())))
                    .orElseGet(HttpResponse::serverError);
        }
        return HttpResponse.serverError();
    }
}
