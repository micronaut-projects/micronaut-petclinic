package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.SQLException;

/** Initializes H2 spatial and ETag functions before the datasource is exposed to schema generation or repositories. */
@Singleton
@Requires(property = "datasources.default.dialect", value = "H2")
@Order(Ordered.HIGHEST_PRECEDENCE) // Run before Micronaut wraps the datasource for transaction-aware access.
public class H2GisInitializer implements BeanCreatedEventListener<DataSource> {

    /**
     * Initializes H2GIS and the ETag alias before schema generation.
     *
     * @param event the newly created datasource
     * @return the initialized datasource
     * @throws IllegalStateException if function initialization fails
     */
    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        DataSource dataSource = event.getBean();
        try (var connection = dataSource.getConnection()) {
            if ("H2".equals(connection.getMetaData().getDatabaseProductName())) {
                try (var statement = connection.createStatement()) {
                    statement.execute("CREATE ALIAS IF NOT EXISTS H2GIS_SPATIAL FOR 'org.h2gis.functions.factory.H2GISFunctions.load'");
                    statement.execute("CALL H2GIS_SPATIAL()");
                    statement.execute("CREATE ALIAS IF NOT EXISTS SYS_ROW_ETAG FOR 'io.micronaut.samples.petclinic.system.VetEtagFunctions.sysRowEtag'");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize H2GIS spatial functions", e);
        }
        return dataSource;
    }
}
