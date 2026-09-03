package io.micronaut.samples.petclinic.repository.h2;

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
import static io.micronaut.samples.petclinic.repository.RepositoryRequirements.DIALECT_H2;

/**
 * H2-backed Micronaut Data repository beans used when no external database environment is active.
 */
public final class H2Repositories {
    private H2Repositories() {
    }

    /**
     * H2 owner repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_H2)
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2OwnerRepository extends OwnerRepository {
    }

    /**
     * H2 pet repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_H2)
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2PetRepository extends PetRepository {
        /**
         * Finds pets for an owner using H2 SQL.
         *
         * @param ownerId the owner id
         * @return pets belonging to the owner
         */
        @Override
        @Query(value = "SELECT p.* FROM PETS p WHERE p.OWNER_ID = :ownerId ORDER BY p.NAME", nativeQuery = true)
        Collection<Pet> findByOwnerId(Integer ownerId);

        /**
         * Finds pets for owners using H2 SQL.
         *
         * @param ownerIds owner ids to match
         * @return pets belonging to the supplied owners
         */
        @Override
        @Query(value = "SELECT p.* FROM PETS p WHERE p.OWNER_ID IN (:ownerIds) ORDER BY p.OWNER_ID, p.NAME", nativeQuery = true)
        List<Pet> findByOwnerIdIn(List<Integer> ownerIds);
    }

    /**
     * H2 pet type repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_H2)
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2PetTypeRepository extends PetTypeRepository {
    }

    /**
     * H2 speciality repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_H2)
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2SpecialityRepository extends SpecialityRepository {
    }

    /**
     * H2 vet repository bean.
     */
    @Requires(notEnv = {"mysql", "postgres", "oracle"})
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2VetRepository extends VetRepository {
        /**
         * Finds all vets and aggregates their specialities using H2 SQL.
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
     * H2 visit repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_H2)
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2VisitRepository extends VisitRepository {
        /**
         * Finds visits for a pet using H2 SQL.
         *
         * @param petId the pet id
         * @return visits for the pet
         */
        @Override
        @Query(value = "SELECT v.* FROM VISITS v WHERE v.PET_ID = :petId ORDER BY v.VISIT_DATE DESC", nativeQuery = true)
        Collection<Visit> findByPetId(Integer petId);
    }

    /**
     * H2 vet-speciality join repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_H2)
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2VetSpecialityRepository extends VetSpecialityRepository {
        /**
         * Finds specialities for a vet using H2 SQL.
         *
         * @param vetId the vet id
         * @return specialities associated with the vet
         */
        @Override
        @Query(value = "SELECT s.* FROM SPECIALTIES s JOIN VET_SPECIALTIES vs ON vs.SPECIALTY_ID = s.id WHERE vs.VET_ID = :vetId ORDER BY s.NAME", nativeQuery = true)
        List<Speciality> findSpecialitiesByVetId(Integer vetId);
    }

    @Requires(notEnv = {"mysql", "postgres", "oracle"})
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2ClinicRepository extends ClinicRepository {
    }

    /**
     * H2 user repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_H2)
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2UserJdbcRepository extends UserJdbcRepository {
    }

    /**
     * H2 role repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_H2)
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2RoleJdbcRepository extends RoleJdbcRepository {
    }

    /**
     * H2 user-role repository bean.
     */
    @Requires(property = DEFAULT_DIALECT_PROPERTY, value = DIALECT_H2)
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2UserRoleJdbcRepository extends UserRoleJdbcRepository {
        /**
         * Finds authority names for a user using H2 SQL.
         *
         * @param username the user name whose authorities should be loaded
         * @return authority names granted to the user
         */
        @Override
        @Query(value = """
                SELECT r.authority FROM `ROLE` r
                INNER JOIN `USER_ROLE` ur ON ur.role_id = r.id
                INNER JOIN `USER` u ON ur.user_id = u.ID
                WHERE u.USERNAME = :username
                """, nativeQuery = true)
        List<String> findAllAuthoritiesByUsername(String username);
    }

}
