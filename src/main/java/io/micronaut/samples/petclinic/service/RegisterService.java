package io.micronaut.samples.petclinic.service;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.samples.petclinic.execption.UserAlreadyExistsException;
import io.micronaut.samples.petclinic.model.Role;
import io.micronaut.samples.petclinic.model.User;
import io.micronaut.samples.petclinic.model.UserRole;
import io.micronaut.samples.petclinic.model.UserRoleId;
import io.micronaut.samples.petclinic.repository.RoleJdbcRepository;
import io.micronaut.samples.petclinic.repository.UserJdbcRepository;
import io.micronaut.samples.petclinic.repository.UserRoleJdbcRepository;
import io.micronaut.samples.petclinic.utils.PasswordEncoder;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates user registration, password encoding, and optional role assignment.
 */
@Singleton
public class RegisterService {

    private static final boolean DEFAULT_ENABLED = true;
    private static final boolean DEFAULT_ACCOUNT_EXPIRED = false;

    private static final boolean DEFAULT_ACCOUNT_LOCKED = false;

    private static final boolean DEFAULT_PASSWORD_EXPIRED = false;
    private final RoleJdbcRepository roleService;
    private final UserJdbcRepository userJdbcRepository;
    private final UserRoleJdbcRepository userRoleJdbcRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the registration service.
     *
     * @param roleJdbcRepository repository used to find or create roles
     * @param userJdbcRepository repository used to persist users
     * @param passwordEncoder encoder used before storing passwords
     * @param userRoleJdbcRepository repository used to persist user-role assignments
     */
    public RegisterService(RoleJdbcRepository roleJdbcRepository,
                           UserJdbcRepository userJdbcRepository,
                           PasswordEncoder passwordEncoder,
                           UserRoleJdbcRepository userRoleJdbcRepository) {
        this.roleService = roleJdbcRepository;
        this.userJdbcRepository = userJdbcRepository;
        this.userRoleJdbcRepository = userRoleJdbcRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a user without assigning any roles.
     *
     * @param username the requested login name
     * @param rawPassword the submitted raw password
     * @throws UserAlreadyExistsException when the user name is already registered
     */
    public void register(@NotBlank String username,
                         @NotBlank String rawPassword) {
        register(username, rawPassword, Collections.emptyList());
    }

    /**
     * Registers a user and assigns the supplied authorities.
     *
     * @param username the requested login name
     * @param rawPassword the submitted raw password
     * @param authorities authorities to assign after the user is created
     * @throws UserAlreadyExistsException when the user name is already registered
     */
    @Transactional
    public void register(@NotBlank String username,
                         @NotBlank String rawPassword,
                         @Nullable List<Role.Authority> authorities) {
        Optional<User> userOptional = userJdbcRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            throw new UserAlreadyExistsException();
        }
        User user = userJdbcRepository.save(createUser(username, rawPassword));
        if (user != null && authorities != null) {
            for (Role.Authority authority : authorities) {
                Role role = roleService.findByAuthority(authority).orElseGet(() -> roleService.save(authority));
                UserRoleId userRoleId = new UserRoleId(user, role);
                if (userRoleJdbcRepository.findById(userRoleId).isEmpty()) {
                    userRoleJdbcRepository.save(new UserRole(userRoleId));
                }
            }
        }
    }

    /**
     * Builds a new enabled user with an encoded password.
     *
     * @param username the requested login name
     * @param rawPassword the submitted raw password
     * @return a user ready to persist
     */
    private User createUser(String username, String rawPassword) {
        final String encodedPassword = passwordEncoder.encode(rawPassword);
        return new User(null,
                username,
                encodedPassword,
                DEFAULT_ENABLED,
                DEFAULT_ACCOUNT_EXPIRED,
                DEFAULT_ACCOUNT_LOCKED,
                DEFAULT_PASSWORD_EXPIRED);
    }
}
