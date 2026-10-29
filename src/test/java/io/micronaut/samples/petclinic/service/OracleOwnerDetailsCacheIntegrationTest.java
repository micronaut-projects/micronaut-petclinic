package io.micronaut.samples.petclinic.service;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.samples.petclinic.ClinicServiceFixtures;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Oracle integration coverage for owner-detail cache keys, serialization, and persistence.
 */
@MicronautTest(transactional = false)
@Requires(env = "oracle")
class OracleOwnerDetailsCacheIntegrationTest {

    private static final String CACHE_NAME = "owner-details";
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(30);

    @Inject
    ClinicService clinicService;

    @Inject
    ClinicServiceFixtures clinicServiceFixtures;

    @Inject
    Environment environment;

    @Test
    void ownerDetailsAreCachedByIdPersistAcrossContextsAndExpire() throws Exception {
        Integer ownerId = clinicServiceFixtures.requiredOwner("Jean", "Coleman").id();
        List<Integer> otherOwnerIds = clinicService.findAllOwners().stream()
                .map(Owner::id)
                .filter(id -> id != null && !id.equals(ownerId))
                .distinct()
                .limit(2)
                .toList();
        assertThat(ownerId).isNotNull();
        assertThat(otherOwnerIds).hasSize(2);

        long startingEntries = entryCount();
        long startingHits = statistic("HIT_COUNT");
        long startingMisses = statistic("MISS_COUNT");

        Owner first = clinicService.findOwnerById(ownerId);
        assertThat(first.id()).isEqualTo(ownerId);
        assertThat(first.getPets()).isNotEmpty();
        assertThat(first.getPets()).anyMatch(pet -> !pet.getVisits().isEmpty());
        awaitEntryCount(startingEntries + 1);

        Owner second = clinicService.findOwnerById(ownerId);
        assertThat(second).isEqualTo(first);
        awaitStatistics(startingHits + 1, startingMisses + 1);

        try (ApplicationContext secondContext = secondContext()) {
            ClinicService serviceFromSecondContext = secondContext.getBean(ClinicService.class);
            assertThat(serviceFromSecondContext.findOwnerById(ownerId)).isEqualTo(first);
            awaitStatistics(startingHits + 2, startingMisses + 1);
        }

        expireEntry(newestEntryHash());
        assertThat(clinicService.findOwnerById(ownerId)).isEqualTo(first);
        awaitStatistics(startingHits + 2, startingMisses + 2);

        clinicService.findOwnerById(otherOwnerIds.get(0));
        awaitEntryCount(startingEntries + 2);
        awaitStatistics(startingHits + 2, startingMisses + 3);

        clinicService.findOwnerById(otherOwnerIds.get(1));
        awaitEntryCount(startingEntries + 3);
        byte[] cleanupKeyHash = newestEntryHash();
        expireEntry(cleanupKeyHash);
        awaitEntryRemoved(cleanupKeyHash);
        awaitStatistics(startingHits + 2, startingMisses + 4);
    }

    private ApplicationContext secondContext() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("datasources.default.url", databaseUrl());
        properties.put("datasources.default.username", environment.getProperty(
                "datasources.default.username", String.class).orElse("petclinic"));
        properties.put("datasources.default.password", environment.getProperty(
                "datasources.default.password", String.class).orElse("petclinic"));
        properties.put("datasources.default.driver-class-name", "oracle.jdbc.OracleDriver");
        properties.put("datasources.default.schema-generate", "NONE");
        properties.put("petclinic.sample-data.enabled", false);
        properties.put("micronaut.server.port", -1);
        return ApplicationContext.builder()
                .environments("oracle")
                .properties(properties)
                .start();
    }

    private long entryCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM PETCLINIC_CACHE_ENTRY WHERE CACHE_NAME = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, CACHE_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private long statistic(String statisticName) throws SQLException {
        String sql = "SELECT " + statisticName + " FROM PETCLINIC_CACHE_STATS WHERE CACHE_NAME = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, CACHE_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        }
    }

    private byte[] newestEntryHash() throws SQLException {
        String sql = "SELECT KEY_HASH FROM PETCLINIC_CACHE_ENTRY "
                + "WHERE CACHE_NAME = ? ORDER BY CREATED_AT DESC FETCH FIRST 1 ROWS ONLY";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, CACHE_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("No Oracle cache entry was written");
                }
                return resultSet.getBytes(1);
            }
        }
    }

    private void expireEntry(byte[] keyHash) throws SQLException {
        String sql = "UPDATE PETCLINIC_CACHE_ENTRY "
                + "SET EXPIRES_AT = SYSTIMESTAMP - INTERVAL '1' SECOND "
                + "WHERE CACHE_NAME = ? AND KEY_HASH = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, CACHE_NAME);
            statement.setBytes(2, keyHash);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Could not mark the Oracle cache entry as expired");
            }
        }
    }

    private boolean entryExists(byte[] keyHash) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PETCLINIC_CACHE_ENTRY WHERE CACHE_NAME = ? AND KEY_HASH = ?";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, CACHE_NAME);
            statement.setBytes(2, keyHash);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private void awaitEntryRemoved(byte[] keyHash) throws Exception {
        await(() -> !entryExists(keyHash), "scheduler cleanup to remove an expired Oracle cache entry");
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                databaseUrl(),
                environment.getProperty("datasources.default.username", String.class).orElse("test"),
                environment.getProperty("datasources.default.password", String.class).orElse("test")
        );
    }

    private String databaseUrl() {
        return environment.getProperty("datasources.default.url", String.class)
                .orElseThrow(() -> new IllegalStateException("Oracle Test Resources did not provide a datasource URL"));
    }

    private void awaitEntryCount(long expectedMinimum) throws Exception {
        await(() -> entryCount() >= expectedMinimum,
                "expected at least " + expectedMinimum + " Oracle cache entries");
    }

    private void awaitStatistics(long expectedHits, long expectedMisses) throws Exception {
        await(() -> statistic("HIT_COUNT") >= expectedHits
                        && statistic("MISS_COUNT") >= expectedMisses,
                "expected Oracle cache hits >= " + expectedHits
                        + " and misses >= " + expectedMisses);
    }

    private void await(SqlCondition condition, String description) throws Exception {
        long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.matches()) {
                return;
            }
            Thread.sleep(100);
        }
        fail("Timed out while waiting for " + description);
    }

    @FunctionalInterface
    private interface SqlCondition {
        boolean matches() throws SQLException;
    }
}
