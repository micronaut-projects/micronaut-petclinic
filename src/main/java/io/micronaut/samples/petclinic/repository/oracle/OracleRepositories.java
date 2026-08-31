package io.micronaut.samples.petclinic.repository.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.samples.petclinic.model.*;
import io.micronaut.samples.petclinic.repository.*;
import java.util.Collection;
import java.util.List;

/**
 * Oracle-backed Micronaut Data repository beans active in the {@code oracle} environment.
 */
public final class OracleRepositories {
    private OracleRepositories() {
    }

    /**
     * Oracle owner repository bean.
     */
    @Requires(env = "oracle")
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
    @Requires(env = "oracle")
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
    @Requires(env = "oracle")
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OraclePetTypeRepository extends PetTypeRepository {
    }

    /**
     * Oracle speciality repository bean.
     */
    @Requires(env = "oracle")
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleSpecialityRepository extends SpecialityRepository {
    }

    /**
     * Oracle vet repository bean.
     */
    @Requires(env = "oracle")
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
    @Requires(env = "oracle")
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleVisitRepository extends VisitRepository {
        /**
         * Finds visits for a pet using Oracle SQL.
         *
         * @param petId the pet id
         * @return visits for the pet
         */
        @Override
        @Query(value = "SELECT v.* FROM VISITS v WHERE v.PET_ID = :petId ORDER BY v.VISIT_DATE DESC", nativeQuery = true)
        Collection<Visit> findByPetId(Integer petId);
    }

    /**
     * Oracle vet-speciality join repository bean.
     */
    @Requires(env = "oracle")
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
}
