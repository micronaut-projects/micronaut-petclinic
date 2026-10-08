package io.micronaut.samples.petclinic.system;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.samples.petclinic.repository.ClinicRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@Requires(property = "datasources.default.dialect", value = "H2")
class H2GisInitializerTest {
    @Inject ClinicRepository clinics;
    @Value("${datasources.default.url}") String jdbcUrl;
    @Value("${datasources.default.username}") String username;
    @Value("${datasources.default.password}") String password;

    @Test
    void openingAnotherConnectionPreservesSpatialFunctions() throws SQLException {
        long originalCount = clinics.count();
        try (var observer = DriverManager.getConnection(jdbcUrl, username, password)) {
            String originalComment = spatialFunctionComment(observer);
            String marker = "H2GIS initialization regression test";
            setSpatialFunctionComment(observer, marker);
            // Force a new physical connection using the application's URL, without pool reuse.
            try (var connection = DriverManager.getConnection(jdbcUrl, username, password)) {
                // Reloading H2GIS replaces this comment, so no timing-dependent race is needed.
                assertThat(spatialFunctionComment(connection)).isEqualTo(marker);
                clinics.saveAll(List.of(
                        new Clinic("H2GIS regression Madison", "15 E Main St.", "Madison", -89.3838, 43.0748),
                        new Clinic("H2GIS regression Monona", "6000 Monona Dr.", "Monona", -89.3240, 43.0622)));
                assertThat(clinics.count()).isEqualTo(originalCount + 2);
            } finally {
                setSpatialFunctionComment(observer, originalComment);
            }
        }
    }

    private String spatialFunctionComment(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT REMARKS FROM INFORMATION_SCHEMA.ROUTINES WHERE ROUTINE_SCHEMA = 'PUBLIC' AND ROUTINE_NAME = 'ST_GEOMFROMGEOJSON'")) {
            assertThat(rows.next()).isTrue();
            return rows.getString(1);
        }
    }

    private void setSpatialFunctionComment(Connection connection, String comment) throws SQLException {
        try (var statement = connection.prepareStatement("COMMENT ON ALIAS ST_GEOMFROMGEOJSON IS ?")) {
            statement.setString(1, comment);
            statement.execute();
        }
    }
}
