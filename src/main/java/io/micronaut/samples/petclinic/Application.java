package io.micronaut.samples.petclinic;

import io.micronaut.runtime.Micronaut;

/**
 * Micronaut Pet Clinic Application.
 * 
 * This is a sample application demonstrating Micronaut features including:
 * - Micronaut Data JDBC for database access
 * - JTE for server-side rendering
 * - Caffeine for caching
 * - Bean validation
 * - Internationalization (i18n)
 * - Multiple database support (H2, MySQL, PostgreSQL)
 * - GraalVM Native Image support
 * 
 * Migrated from the Spring Pet Clinic sample application.
 */
public class Application {

    /**
     * Creates the application bootstrap type.
     */
    public Application() {
    }

    /**
     * Starts the Micronaut Pet Clinic application.
     *
     * @param args command-line arguments passed to Micronaut
     */
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
