package io.micronaut.samples.petclinic.repository.postgres;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Sort;
import io.micronaut.data.model.geo.Geometry;
import io.micronaut.data.model.geo.Point;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.samples.petclinic.model.Clinic;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.VetWithSpecialities;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.repository.ClinicRepository;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.repository.PetRepository;
import io.micronaut.samples.petclinic.repository.PetTypeRepository;
import io.micronaut.samples.petclinic.repository.SpecialityRepository;
import io.micronaut.samples.petclinic.repository.VetRepository;
import io.micronaut.samples.petclinic.repository.VetSpecialityRepository;
import io.micronaut.samples.petclinic.repository.VisitRepository;

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
        @Query(value = """
                SELECT p.* FROM "PETS" p WHERE p."OWNER_ID" = :ownerId ORDER BY p."NAME"
                """, nativeQuery = true)
        Collection<Pet> findByOwnerId(Integer ownerId);

        /**
         * Finds pets for owners using PostgreSQL SQL.
         *
         * @param ownerIds owner ids to match
         * @return pets belonging to the supplied owners
         */
        @Override
        @Query(value = """
                SELECT p.* FROM "PETS" p WHERE p."OWNER_ID" IN (:ownerIds) ORDER BY p."OWNER_ID", p."NAME"
                """, nativeQuery = true)
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
                    v."FIRST_NAME" AS first_name,
                    v."LAST_NAME" AS last_name,
                    STRING_AGG(
                        CASE WHEN s.id IS NOT NULL THEN s.id::text || ':' || s."NAME" END,
                        '|' ORDER BY s."NAME"
                    ) AS speciality_rows
                FROM "VETS" v
                LEFT JOIN "VET_SPECIALTIES" vs ON vs."VET_ID" = v.id
                LEFT JOIN "SPECIALTIES" s ON s.id = vs."SPECIALTY_ID"
                GROUP BY v.id, v."FIRST_NAME", v."LAST_NAME"
                ORDER BY v."LAST_NAME"
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
        @Query(value = """
            SELECT v.* FROM "VISITS" v WHERE v."PET_ID" = :petId ORDER BY v."VISIT_DATE" DESC
                        """, nativeQuery = true)
        Collection<Visit> findByPetId(Integer petId);

        /**
         * Finds visits for a pet using Oracle SQL.
         *
         * @param petName the pet name
         * @return visits for the pet
         */
        @Override
        @Query(value = "SELECT v.* FROM VISITS v LEFT JOIN PETS p ON v.PET_ID = p.ID WHERE p.NAME = :petName  ORDER BY v.VISIT_DATE DESC", nativeQuery = true)
        Collection<Visit> findByPetName(String petName);
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
        @Query(value = """
                SELECT s.* FROM "SPECIALTIES" s JOIN "VET_SPECIALTIES" vs ON vs."SPECIALTY_ID" = s.id WHERE vs."VET_ID" = :vetId ORDER BY s."NAME"
                """, nativeQuery = true)
        List<Speciality> findSpecialitiesByVetId(Integer vetId);
    }

    @Requires(env = "postgres")
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresClinicRepository extends ClinicRepository {
        // workaround issue https://github.com/micronaut-projects/micronaut-data/issues/3991
        /**
         * PostGIS geometry columns use coordinate units (degrees for WGS 84),
         * while the application API expresses the radius in meters. Casting
         * both operands to geography makes ST_DWithin use meters.
         */
        @Query(value = """
                SELECT
                    c."id",
                    c."NAME",
                    c."ADDRESS",
                    c."CITY",
                    ST_AsGeoJSON(c."LOCATION") AS "LOCATION",
                    ST_AsGeoJSON(c."SERVICE_AREA") AS "SERVICE_AREA"
                FROM "CLINICS" c
                WHERE ST_DWithin(
                    c."LOCATION"::geography,
                    ST_GeomFromText(:wkt, 4326)::geography,
                    :distance
                )
                """, nativeQuery = true)
        List<Clinic> findByLocationNearInMeters(String wkt, double distance);

        @Override
        default List<Clinic> findByLocationNear(Geometry geometry, double distance) {
            if (!(geometry instanceof Point(double x, double y))) {
                throw new IllegalArgumentException("PostgreSQL nearby searches require a Point");
            }
            return findByLocationNearInMeters("POINT (" + x + " " + y + ")", distance);
        }
    }
}
