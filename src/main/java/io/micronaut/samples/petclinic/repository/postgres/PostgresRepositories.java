package io.micronaut.samples.petclinic.repository.postgres;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.samples.petclinic.model.*;
import io.micronaut.samples.petclinic.repository.*;
import java.util.Collection;
import java.util.List;

/**
 * PostgreSQL-backed Micronaut Data repository beans active in the {@code postgres} environment.
 */
public final class PostgresRepositories {
    private PostgresRepositories() {
    }

    /**
     * PostgreSQL owner repository bean.
     */
    @Requires(env = "postgres")
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresOwnerRepository extends OwnerRepository {
    }

    /**
     * PostgreSQL pet repository bean.
     */
    @Requires(env = "postgres")
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
    @Requires(env = "postgres")
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresPetTypeRepository extends PetTypeRepository {
    }

    /**
     * PostgreSQL speciality repository bean.
     */
    @Requires(env = "postgres")
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresSpecialityRepository extends SpecialityRepository {
    }

    /**
     * PostgreSQL vet repository bean.
     */
    @Requires(env = "postgres")
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
    @Requires(env = "postgres")
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
    @Requires(env = "postgres")
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
}
