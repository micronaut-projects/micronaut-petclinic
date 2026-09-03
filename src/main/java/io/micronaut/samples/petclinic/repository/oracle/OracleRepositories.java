package io.micronaut.samples.petclinic.repository.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.VetWithSpecialities;
import io.micronaut.samples.petclinic.repository.ClinicRepository;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.repository.PetCareChunkRepository;
import io.micronaut.samples.petclinic.repository.PetCareDocumentRepository;
import io.micronaut.samples.petclinic.repository.PetRepository;
import io.micronaut.samples.petclinic.repository.PetTypeRepository;
import io.micronaut.samples.petclinic.repository.RoleJdbcRepository;
import io.micronaut.samples.petclinic.repository.SpecialityRepository;
import io.micronaut.samples.petclinic.repository.UserJdbcRepository;
import io.micronaut.samples.petclinic.repository.UserRoleJdbcRepository;
import io.micronaut.samples.petclinic.repository.VetRepository;
import io.micronaut.samples.petclinic.repository.VetSpecialityRepository;
import io.micronaut.samples.petclinic.repository.VisitRepository;

import java.util.Collection;
import java.util.List;

import static io.micronaut.samples.petclinic.repository.RepositoryRequirements.DEFAULT_DIALECT_PROPERTY;
import static io.micronaut.samples.petclinic.repository.RepositoryRequirements.DIALECT_ORACLE;

/**
 * Oracle-backed Micronaut Data repository beans active in the {@code oracle} environment.
 */
public final class OracleRepositories {
    private OracleRepositories() {
    }

    /**
     * Oracle owner repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleOwnerRepository extends OwnerRepository {
    }

    /**
     * Oracle clinic repository bean.
     */
    @Requires(env = "oracle")
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleClinicRepository extends ClinicRepository {
    }

    /**
     * Oracle pet-care document repository bean.
     */
    @Requires(env = "oracle")
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OraclePetCareDocumentRepository extends PetCareDocumentRepository {
    }

    /**
     * Oracle vector-searchable pet-care chunk repository bean.
     */
    @Requires(env = "oracle")
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OraclePetCareChunkRepository extends PetCareChunkRepository {
    }

    /**
     * Oracle pet repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OraclePetRepository extends PetRepository {
        /**
         * Finds pets for an owner using Oracle SQL.
         *
         * @param ownerId the owner id
         * @return pets belonging to the owner
         */
        @Override
        @Query(value = "SELECT p.* FROM PETS p WHERE p.OWNER_ID = :ownerId ORDER BY p.NAME", nativeQuery = true)
        Collection<Pet> findByOwnerId(Integer ownerId);

        /**
         * Finds pets for owners using Oracle SQL.
         *
         * @param ownerIds owner ids to match
         * @return pets belonging to the supplied owners
         */
        @Override
        @Query(value = "SELECT p.* FROM PETS p WHERE p.OWNER_ID IN (:ownerIds) ORDER BY p.OWNER_ID, p.NAME", nativeQuery = true)
        List<Pet> findByOwnerIdIn(List<Integer> ownerIds);
    }

    /**
     * Oracle pet type repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OraclePetTypeRepository extends PetTypeRepository {
    }

    /**
     * Oracle speciality repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleSpecialityRepository extends SpecialityRepository {
    }

    /**
     * Oracle vet repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleVetRepository extends VetRepository {
        /**
         * Finds all vets and aggregates their specialities using Oracle SQL.
         *
         * @return all vets ordered by last name with aggregated speciality rows
         */
        @Override
        @Query(value = """
                SELECT
                    v.id,
                    v.FIRST_NAME AS first_name,
                    v.LAST_NAME AS last_name,
                    LISTAGG(
                        CASE WHEN s.id IS NOT NULL THEN s.id || ':' || s.NAME END,
                        '|'
                    ) WITHIN GROUP (ORDER BY s.NAME) AS speciality_rows
                FROM VETS v
                LEFT JOIN VET_SPECIALTIES vs ON vs.VET_ID = v.id
                LEFT JOIN SPECIALTIES s ON s.id = vs.SPECIALTY_ID
                GROUP BY v.id, v.FIRST_NAME, v.LAST_NAME
                ORDER BY v.LAST_NAME
                """, nativeQuery = true)
        Collection<VetWithSpecialities> findAllWithSpecialities();
    }

    /**
     * Oracle visit repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleVisitRepository extends VisitRepository {
    }

    /**
     * Oracle vet-speciality join repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleVetSpecialityRepository extends VetSpecialityRepository {
        /**
         * Finds specialities for a vet using Oracle SQL.
         *
         * @param vetId the vet id
         * @return specialities associated with the vet
         */
        @Override
        @Query(value = "SELECT s.* FROM SPECIALTIES s JOIN VET_SPECIALTIES vs ON vs.SPECIALTY_ID = s.id WHERE vs.VET_ID = :vetId ORDER BY s.NAME", nativeQuery = true)
        List<Speciality> findSpecialitiesByVetId(Integer vetId);
    }

    /**
     * Oracle user repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleUserJdbcRepository extends UserJdbcRepository {
    }

    /**
     * Oracle role repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleRoleJdbcRepository extends RoleJdbcRepository {
    }

    /**
     * Oracle user-role repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_ORACLE)
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleUserRoleJdbcRepository extends UserRoleJdbcRepository {
        /**
         * Finds authority names for a user using Oracle SQL.
         *
         * @param username the user name whose authorities should be loaded
         * @return authority names granted to the user
         */
        @Override
        @Query(value = """
                SELECT r.authority FROM "ROLE" r
                INNER JOIN "USER_ROLE" ur ON ur.role_id = r.id
                INNER JOIN "USER" u ON ur.user_id = u."ID"
                WHERE u."USERNAME" = :username
                """, nativeQuery = true)
        List<String> findAllAuthoritiesByUsername(String username);
    }
}
