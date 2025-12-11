package io.micronaut.samples.petclinic;

import io.micronaut.runtime.Micronaut;

/**
 * Micronaut Pet Clinic Application.
 * 
 * This is a sample application demonstrating Micronaut features including:
 * - Micronaut Data JPA for database access
 * - Thymeleaf for server-side rendering
 * - Caffeine for caching
 * - Bean validation
 * - Internationalization (i18n)
 * - Multiple database support (H2, MySQL, PostgreSQL)
 * - GraalVM Native Image support
 * 
 * Migrated from the Spring Pet Clinic sample application.
 */
public class Application {
    
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
