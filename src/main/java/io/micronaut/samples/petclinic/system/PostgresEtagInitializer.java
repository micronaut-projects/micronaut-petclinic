package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.SQLException;

/** Initializes the PostgreSQL ETag function before repositories use the datasource. */
@Singleton
@Requires(property = "datasources.default.dialect", value = "POSTGRES")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PostgresEtagInitializer implements BeanCreatedEventListener<DataSource> {

    // Keep this positional signature aligned with Vet's ETag inputs: id, firstName, lastName, specialitiesHash.
    private static final String CREATE_ETAG_FUNCTION = """
            CREATE OR REPLACE FUNCTION SYS_ROW_ETAG(INTEGER, VARCHAR, VARCHAR, VARCHAR)
            RETURNS VARCHAR
            LANGUAGE SQL
            IMMUTABLE
            AS $$
            SELECT upper(md5(
                CASE WHEN $1 IS NULL THEN '-1:' ELSE length($1::text)::text || ':' || $1::text END ||
                CASE WHEN $2 IS NULL THEN '-1:' ELSE length($2)::text || ':' || $2 END ||
                CASE WHEN $3 IS NULL THEN '-1:' ELSE length($3)::text || ':' || $3 END ||
                CASE WHEN $4 IS NULL THEN '-1:' ELSE length($4)::text || ':' || $4 END
            ))::varchar
            $$
            """;

    /**
     * Creates or replaces the ETag function once when the PostgreSQL datasource is created.
     *
     * @param event the newly created datasource
     * @return the initialized datasource
     * @throws IllegalStateException if the function cannot be created
     */
    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        DataSource dataSource = event.getBean();
        try (var connection = dataSource.getConnection()) {
            if ("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                try (var statement = connection.createStatement()) {
                    statement.execute(CREATE_ETAG_FUNCTION);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize the PostgreSQL ETag function", e);
        }
        return dataSource;
    }
}
