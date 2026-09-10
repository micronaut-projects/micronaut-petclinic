package io.micronaut.samples.petclinic.repository.mysql;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.samples.petclinic.model.*;
import io.micronaut.samples.petclinic.repository.*;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * MySQL-backed Micronaut Data repository beans active in the {@code mysql} environment.
 */
public final class MySqlRepositories {
    private MySqlRepositories() {
    }

    /**
     * MySQL owner repository bean.
     */
    @Requires(env = "mysql")
    @JdbcRepository(dialect = Dialect.MYSQL)
    public interface MySqlOwnerRepository extends OwnerRepository {
    }

    /**
     * MySQL pet repository bean.
     */
    @Requires(env = "mysql")
    @JdbcRepository(dialect = Dialect.MYSQL)
    public interface MySqlPetRepository extends PetRepository {
        /**
         * Finds pets for an owner using MySQL SQL.
         *
         * @param ownerId the owner id
         * @return pets belonging to the owner
         */
        @Override
        @Query(value = "SELECT p.* FROM PETS p WHERE p.OWNER_ID = :ownerId ORDER BY p.NAME", nativeQuery = true)
        Collection<Pet> findByOwnerId(Integer ownerId);

        /**
         * Finds pets for owners using MySQL SQL.
         *
         * @param ownerIds owner ids to match
         * @return pets belonging to the supplied owners
         */
        @Override
        @Query(value = "SELECT p.* FROM PETS p WHERE p.OWNER_ID IN (:ownerIds) ORDER BY p.OWNER_ID, p.NAME", nativeQuery = true)
        List<Pet> findByOwnerIdIn(List<Integer> ownerIds);
    }

    /**
     * MySQL pet type repository bean.
     */
    @Requires(env = "mysql")
    @JdbcRepository(dialect = Dialect.MYSQL)
    public interface MySqlPetTypeRepository extends PetTypeRepository {
    }

    /**
     * MySQL speciality repository bean.
     */
    @Requires(env = "mysql")
    @JdbcRepository(dialect = Dialect.MYSQL)
    public interface MySqlSpecialityRepository extends SpecialityRepository {
    }

    /**
     * MySQL vet repository bean.
     */
    @Requires(env = "mysql")
    @JdbcRepository(dialect = Dialect.MYSQL)
    public interface MySqlVetRepository extends VetRepository {
        /**
         * Finds all vets and aggregates their specialities using MySQL SQL.
         *
         * @return all vets ordered by last name with aggregated speciality rows
         */
        @Override
        @Query(value = """
                SELECT
                    v.id,
                    v.FIRST_NAME AS first_name,
                    v.LAST_NAME AS last_name,
                    GROUP_CONCAT(
                        CASE WHEN s.id IS NOT NULL THEN CONCAT(s.id, ':', s.NAME) END
                        ORDER BY s.NAME SEPARATOR '|'
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
     * MySQL visit repository bean.
     */
    @Requires(env = "mysql")
    @JdbcRepository(dialect = Dialect.MYSQL)
    public interface MySqlVisitRepository extends VisitRepository {
        /**
         * Finds visits for a pet using MySQL SQL.
         *
         * @param petId the pet id
         * @return visits for the pet
         */
        @Override
        @Query(value = "SELECT v.* FROM VISITS v WHERE v.PET_ID = :petId ORDER BY v.VISIT_DATE DESC", nativeQuery = true)
        Collection<Visit> findByPetId(Integer petId);

        @Override
        @Query(value = "SELECT v.* FROM VISITS v WHERE v.VISIT_DATE = :date ORDER BY v.PET_ID, v.id", nativeQuery = true)
        Collection<Visit> findByDate(LocalDate date);
    }

    /**
     * MySQL vet-speciality join repository bean.
     */
    @Requires(env = "mysql")
    @JdbcRepository(dialect = Dialect.MYSQL)
    public interface MySqlVetSpecialityRepository extends VetSpecialityRepository {
        /**
         * Finds specialities for a vet using MySQL SQL.
         *
         * @param vetId the vet id
         * @return specialities associated with the vet
         */
        @Override
        @Query(value = "SELECT s.* FROM SPECIALTIES s JOIN VET_SPECIALTIES vs ON vs.SPECIALTY_ID = s.id WHERE vs.VET_ID = :vetId ORDER BY s.NAME", nativeQuery = true)
        List<Speciality> findSpecialitiesByVetId(Integer vetId);
    }

    @Requires(env = "mysql")
    @JdbcRepository(dialect = Dialect.MYSQL)
    public interface MySqlClinicRepository extends ClinicRepository {
    }
}
