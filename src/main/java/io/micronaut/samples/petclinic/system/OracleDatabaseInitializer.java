package io.micronaut.samples.petclinic.system;

import io.micronaut.context.ApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * Initializes the Oracle PetClinic schema for the Compose DeepSec bootstrap.
 *
 * <p>The normal application can use the local {@code petclinic} user to create
 * tables and sample data. Deep Data Security requests use a different,
 * token-authenticated connection and must not run that startup loader without
 * an end-user token. This small entry point keeps those two identities
 * separate while allowing Compose to run initialization automatically.</p>
 */
public final class OracleDatabaseInitializer {

    private static final String DEFAULT_URL = "jdbc:oracle:thin:@oracle:1521/FREEPDB1";
    private static final String DEFAULT_USERNAME = "petclinic";
    private static final String DEFAULT_PASSWORD = "petclinic";

    private OracleDatabaseInitializer() {
    }

    /**
     * Creates and seeds the schema when it has not already been initialized.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        String url = environment("DATASOURCES_DEFAULT_URL", DEFAULT_URL);
        String username = environment("DATASOURCES_DEFAULT_USERNAME", DEFAULT_USERNAME);
        String password = environment("DATASOURCES_DEFAULT_PASSWORD", DEFAULT_PASSWORD);

        if (hasSampleData(url, username, password)) {
            System.out.println("Oracle PetClinic schema already contains sample data; bootstrap skipped.");
            return;
        }

        System.out.println("Initializing Oracle PetClinic schema and sample data...");
        Map<String, Object> properties = Map.of(
                "datasources.default.url", url,
                "datasources.default.username", username,
                "datasources.default.password", password,
                "datasources.default.schema-generate", "CREATE"
        );
        try (ApplicationContext context = ApplicationContext.builder()
                .environments("oracle")
                .properties(properties)
                .start()) {
            System.out.println("Oracle PetClinic schema and sample data initialized.");
        }
    }

    private static boolean hasSampleData(String url, String username, String password) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            try (Connection connection = DriverManager.getConnection(url, username, password);
                 PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM OWNERS");
                 ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Oracle JDBC driver is not available", exception);
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 942) {
                return false;
            }
            throw new IllegalStateException("Could not inspect the Oracle PetClinic schema", exception);
        }
    }

    private static String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
