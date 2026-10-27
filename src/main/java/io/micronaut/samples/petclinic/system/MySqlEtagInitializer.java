package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.SQLException;

/** Initializes the MySQL ETag function before repositories use the datasource. */
@Singleton
@Requires(property = "datasources.default.dialect", value = "mysql")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MySqlEtagInitializer implements BeanCreatedEventListener<DataSource> {

    // Keep this positional signature aligned with Vet's ETag inputs: id, firstName, lastName, specialitiesHash.
    private static final String CREATE_ETAG_FUNCTION = """
            CREATE FUNCTION SYS_ROW_ETAG(
                vet_id INT,
                first_name VARCHAR(255),
                last_name VARCHAR(255),
                specialities_hash VARCHAR(64)
            )
            RETURNS CHAR(32) CHARACTER SET ascii
            DETERMINISTIC
            RETURN UPPER(LEFT(SHA2(CONCAT(
                IF(vet_id IS NULL, '-1:', CONCAT(CHAR_LENGTH(CAST(vet_id AS CHAR)), ':', CAST(vet_id AS CHAR))),
                IF(first_name IS NULL, '-1:', CONCAT(CHAR_LENGTH(first_name), ':', first_name)),
                IF(last_name IS NULL, '-1:', CONCAT(CHAR_LENGTH(last_name), ':', last_name)),
                IF(specialities_hash IS NULL, '-1:', CONCAT(CHAR_LENGTH(specialities_hash), ':', specialities_hash))
            ), 256), 32))
            """;

    private static final String DROP_ETAG_FUNCTION = "DROP FUNCTION IF EXISTS SYS_ROW_ETAG";

    /**
     * Recreates the ETag function when the MySQL datasource is created.
     *
     * @param event the newly created datasource
     * @return the initialized datasource
     * @throws IllegalStateException if the function cannot be created
     */
    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        return initialize(event.getBean());
    }

    DataSource initialize(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            if ("MySQL".equals(connection.getMetaData().getDatabaseProductName())) {
                try (var statement = connection.createStatement()) {
                    statement.execute(DROP_ETAG_FUNCTION);
                    statement.execute(CREATE_ETAG_FUNCTION);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize the MySQL ETag function", e);
        }
        return dataSource;
    }
}
