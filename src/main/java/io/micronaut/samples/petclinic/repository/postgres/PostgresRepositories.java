package io.micronaut.samples.petclinic.repository.postgres;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.VetWithSpecialities;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.repository.ClinicRepository;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
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
import static io.micronaut.samples.petclinic.repository.RepositoryRequirements.DIALECT_POSTGRES;

/**
 * PostgreSQL-backed Micronaut Data repository beans active in the {@code postgres} environment.
 */
public final class PostgresRepositories {
    private PostgresRepositories() {
    }

    /**
     * PostgreSQL owner repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresOwnerRepository extends OwnerRepository {
    }

    /**
     * PostgreSQL pet repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresPetRepository extends PetRepository {
        /**
         * Finds pets for an owner using PostgreSQL SQL.
         *
         * @param ownerId the owner id
         * @return pets belonging to the owner
         */
        @Override
        @Query(value = "SELECT p.* FROM PETS p WHERE p.OWNER_ID = :ownerId ORDER BY p.NAME", nativeQuery = true)
        Collection<Pet> findByOwnerId(Integer ownerId);

        /**
         * Finds pets for owners using PostgreSQL SQL.
         *
         * @param ownerIds owner ids to match
         * @return pets belonging to the supplied owners
         */
        @Override
        @Query(value = "SELECT p.* FROM PETS p WHERE p.OWNER_ID IN (:ownerIds) ORDER BY p.OWNER_ID, p.NAME", nativeQuery = true)
        List<Pet> findByOwnerIdIn(List<Integer> ownerIds);
    }

    /**
     * PostgreSQL pet type repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresPetTypeRepository extends PetTypeRepository {
    }

    /**
     * PostgreSQL speciality repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresSpecialityRepository extends SpecialityRepository {
    }

    /**
     * PostgreSQL vet repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresVetRepository extends VetRepository {
        /**
         * Finds all vets and aggregates their specialities using PostgreSQL SQL.
         *
         * @return all vets ordered by last name with aggregated speciality rows
         */
        @Override
        @Query(value = """
                SELECT
                    v.id,
                    v.FIRST_NAME AS first_name,
                    v.LAST_NAME AS last_name,
                    STRING_AGG(
                        CASE WHEN s.id IS NOT NULL THEN s.id::text || ':' || s.NAME END,
                        '|' ORDER BY s.NAME
                    ) AS speciality_rows
                FROM VETS v
                LEFT JOIN VET_SPECIALTIES vs ON vs.VET_ID = v.id
                LEFT JOIN SPECIALTIES s ON s.id = vs.SPECIALTY_ID
                GROUP BY v.id, v.FIRST_NAME, v.LAST_NAME
                ORDER BY v.LAST_NAME
                """, nativeQuery = true)
        Collection<VetWithSpecialities> findAllWithSpecialities();
    }

    /**
     * PostgreSQL visit repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresVisitRepository extends VisitRepository {
        /**
         * Finds visits for a pet using PostgreSQL SQL.
         *
         * @param petId the pet id
         * @return visits for the pet
         */
        @Override
        @Query(value = "SELECT v.* FROM VISITS v WHERE v.PET_ID = :petId ORDER BY v.VISIT_DATE DESC", nativeQuery = true)
        Collection<Visit> findByPetId(Integer petId);
    }

    /**
     * PostgreSQL vet-speciality join repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresVetSpecialityRepository extends VetSpecialityRepository {
        /**
         * Finds specialities for a vet using PostgreSQL SQL.
         *
         * @param vetId the vet id
         * @return specialities associated with the vet
         */
        @Override
        @Query(value = "SELECT s.* FROM SPECIALTIES s JOIN VET_SPECIALTIES vs ON vs.SPECIALTY_ID = s.id WHERE vs.VET_ID = :vetId ORDER BY s.NAME", nativeQuery = true)
        List<Speciality> findSpecialitiesByVetId(Integer vetId);
    }

    /**
     * PostgreSQL user repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgesUserJdbcRepository extends UserJdbcRepository {
    }

    /**
     * PostgreSQL role repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgesRoleJdbcRepository extends RoleJdbcRepository {
    }

    /**
     * PostgreSQL user-role repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgesUserRoleJdbcRepository extends UserRoleJdbcRepository {
        /**
         * Finds authority names for a user using PostgreSQL SQL.
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

    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_POSTGRES)
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresClinicRepository extends ClinicRepository {
    }
}
