package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.connection.annotation.Connectable;
import jakarta.inject.Singleton;
import io.micronaut.samples.petclinic.repository.oracle.OwnerLoyaltyAccountRepository;
import io.micronaut.transaction.TransactionOperations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Raw Oracle SQL helpers used to verify generated schema rules and rollback behavior. */
@Singleton
@Requires(env = "oracle", notEnv = "oracle-deepsec")
public class OracleLoyaltyTestDatabase {

    private final DataSource dataSource;
    private final OwnerLoyaltyAccountRepository accountRepository;
    private final TransactionOperations<Connection> transactions;

    public OracleLoyaltyTestDatabase(DataSource dataSource, OwnerLoyaltyAccountRepository accountRepository,
                                     TransactionOperations<Connection> transactions) {
        this.dataSource = dataSource;
        this.accountRepository = accountRepository;
        this.transactions = transactions;
    }

    /** Holds a reservation open in an independent transaction until the test releases it. */
    public void holdReservation(long accountId, long delta, CountDownLatch reserved,
                                CountDownLatch release, boolean commit) {
        transactions.executeWrite(status -> {
            long affected = delta < 0
                    ? accountRepository.reserveDecrementPoints(accountId, -delta)
                    : accountRepository.reserveIncrementPoints(accountId, delta);
            if (affected != 1) {
                throw new IllegalStateException("The test account is missing.");
            }
            reserved.countDown();
            try {
                if (!release.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("The test did not release its reservation.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("The reservation test was interrupted.", e);
            }
            if (!commit) {
                status.setRollbackOnly();
            }
            return null;
        });
    }

    @Connectable
    public String reservablePointsColumn() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT RESERVABLE_COLUMN FROM USER_TAB_COLUMNS
                     WHERE TABLE_NAME = 'OWNER_LOYALTY_ACCOUNT' AND COLUMN_NAME = 'POINTS'
                     """);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getString(1) : null;
        }
    }

    @Connectable
    public boolean pointsColumnIsNotNullable() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT NULLABLE FROM USER_TAB_COLUMNS
                     WHERE TABLE_NAME = 'OWNER_LOYALTY_ACCOUNT' AND COLUMN_NAME = 'POINTS'
                     """);
             ResultSet result = statement.executeQuery()) {
            return result.next() && "N".equalsIgnoreCase(result.getString(1));
        }
    }

    @Connectable
    public int nonnegativeCheckConstraintCount() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*) FROM USER_CONSTRAINTS
                     WHERE TABLE_NAME = 'OWNER_LOYALTY_ACCOUNT' AND CONSTRAINT_TYPE = 'C'
                       AND UPPER(SEARCH_CONDITION_VC) LIKE '%POINTS%>= 0%'
                     """);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    @Connectable
    public int uniqueOwnerRelationIndexCount() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*) FROM USER_IND_COLUMNS c
                     JOIN USER_INDEXES i ON i.INDEX_NAME = c.INDEX_NAME
                     WHERE c.TABLE_NAME = 'OWNER_LOYALTY_ACCOUNT'
                       AND c.COLUMN_NAME = 'OWNER_ID' AND i.UNIQUENESS = 'UNIQUE'
                     """);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    @Connectable
    public void directAssignPoints(int ownerId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE OWNER_LOYALTY_ACCOUNT SET POINTS = 500 WHERE OWNER_ID = ?")) {
            statement.setInt(1, ownerId);
            statement.executeUpdate();
        }
    }

    @Connectable
    public void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
