package io.micronaut.samples.petclinic.repository;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Vet;

/**
 * Repository queries needed only by integration tests.
 */
public final class TestRepositories {
    private TestRepositories() {
    }

    public interface VetTestRepository extends CrudRepository<Vet, Integer> {
        @Query(value = "SELECT COUNT(*) FROM VET_SPECIALTIES WHERE VET_ID = :id", nativeQuery = true)
        long countSpecialityLinks(Integer id);
    }

    @Requires(notEnv = {"mysql", "postgres", "oracle", "oracle-deepsec"})
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2VetLinkCountRepository extends VetTestRepository {
    }

    @Requires(env = "mysql")
    @JdbcRepository(dialect = Dialect.MYSQL)
    public interface MySqlVetLinkCountRepository extends VetTestRepository {
    }

    @Requires(env = {"oracle", "oracle-deepsec"})
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleVetLinkCountRepository extends VetTestRepository {
    }

    @Requires(env = "postgres")
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresVetLinkCountRepository extends VetTestRepository {
        @Override
        @Query(value = """
                SELECT COUNT(*) FROM "VET_SPECIALTIES" WHERE "VET_ID" = :id
                """, nativeQuery = true)
        long countSpecialityLinks(Integer id);
    }
}
