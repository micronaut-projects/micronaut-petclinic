package io.micronaut.samples.petclinic.repository.postgres;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.samples.petclinic.repository.ClinicRepository;
import io.micronaut.samples.petclinic.repository.ClinicServiceOfferingRepository;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.repository.PetRepository;
import io.micronaut.samples.petclinic.repository.PetTypeRepository;
import io.micronaut.samples.petclinic.repository.SpecialityRepository;
import io.micronaut.samples.petclinic.repository.VetRepository;
import io.micronaut.samples.petclinic.repository.VisitRepository;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
        @Join(value = "type", type = Join.Type.LEFT_FETCH)
        @Query(value = """
                SELECT p.*, pet_type_."id" AS type_id, pet_type_."NAME" AS type_NAME
                FROM "PETS" p
                LEFT JOIN "TYPES" pet_type_ ON p."TYPE_ID" = pet_type_."id"
                WHERE p."OWNER_ID" = :ownerId
                ORDER BY p."NAME"
                """, nativeQuery = true)
        Collection<Pet> findByOwnerId(Integer ownerId);

        /**
         * Finds pets for owners using PostgreSQL SQL.
         *
         * @param ownerIds owner ids to match
         * @return pets belonging to the supplied owners
         */
        @Join(value = "type", type = Join.Type.LEFT_FETCH)
        @Query(value = """
                SELECT p.*, pet_type_."id" AS type_id, pet_type_."NAME" AS type_NAME
                FROM "PETS" p
                LEFT JOIN "TYPES" pet_type_ ON p."TYPE_ID" = pet_type_."id"
                WHERE p."OWNER_ID" IN (:ownerIds)
                ORDER BY p."OWNER_ID", p."NAME"
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
         * Finds veterinarians and their specialities using PostgreSQL-safe ETag column quoting.
         *
         * @return veterinarians ordered by last name with their ETags and specialities
         */
        @Override
        @Join(value = "specialities", type = Join.Type.LEFT_FETCH)
        @Query(value = """
                SELECT vet_."id",
                       SYS_ROW_ETAG(vet_."id", vet_."FIRST_NAME", vet_."LAST_NAME", vet_."SPECIALITIES_ETAG_HASH") AS etag,
                       vet_."FIRST_NAME",
                       vet_."LAST_NAME",
                       vet_."SPECIALITIES_ETAG_HASH",
                       vet_specialities_."id" AS specialities_id,
                       vet_specialities_."NAME" AS specialities_NAME
                FROM "VETS" vet_
                LEFT JOIN "VET_SPECIALTIES" vet_specialities_VET_SPECIALTIES_
                       ON vet_."id" = vet_specialities_VET_SPECIALTIES_."VET_ID"
                LEFT JOIN "SPECIALTIES" vet_specialities_
                       ON vet_specialities_VET_SPECIALTIES_."SPECIALTY_ID" = vet_specialities_."id"
                ORDER BY vet_."LAST_NAME" ASC
                """, nativeQuery = true)
        List<Vet> findAll();

        /**
         * Finds one veterinarian and its specialities using PostgreSQL-safe ETag column quoting.
         *
         * @param id veterinarian identifier
         * @return the veterinarian, or empty if it does not exist
         */
        @Override
        @Join(value = "specialities", type = Join.Type.LEFT_FETCH)
        @Query(value = """
                SELECT vet_."id",
                       SYS_ROW_ETAG(vet_."id", vet_."FIRST_NAME", vet_."LAST_NAME", vet_."SPECIALITIES_ETAG_HASH") AS etag,
                       vet_."FIRST_NAME",
                       vet_."LAST_NAME",
                       vet_."SPECIALITIES_ETAG_HASH",
                       vet_specialities_."id" AS specialities_id,
                       vet_specialities_."NAME" AS specialities_NAME
                FROM "VETS" vet_
                LEFT JOIN "VET_SPECIALTIES" vet_specialities_VET_SPECIALTIES_
                       ON vet_."id" = vet_specialities_VET_SPECIALTIES_."VET_ID"
                LEFT JOIN "SPECIALTIES" vet_specialities_
                       ON vet_specialities_VET_SPECIALTIES_."SPECIALTY_ID" = vet_specialities_."id"
                WHERE vet_."id" = :id
                """, nativeQuery = true)
        Optional<Vet> findById(Integer id);

        /**
         * {@inheritDoc}
         */
        @Override
        @Query(value = """
                DELETE FROM "VET_SPECIALTIES" WHERE "VET_ID" = :id
                """, nativeQuery = true)
        void deleteSpecialityLinks(Integer id);

        /**
         * {@inheritDoc}
         */
        @Override
        @Query(value = """
                INSERT INTO "VET_SPECIALTIES" ("VET_ID", "SPECIALTY_ID") VALUES (:vetId, :specialityId)
                """, nativeQuery = true)
        void insertSpecialityLink(Integer vetId, Integer specialityId);
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
    @Query(value = """
        SELECT v.* FROM "VISITS" v LEFT JOIN "PETS" p ON v."PET_ID" = p.ID WHERE p."NAME" = :petName ORDER BY v."VISIT_DATE" DESC
        """, nativeQuery = true)
    Collection<Visit> findByPetName(String petName);
  }

    @Requires(env = "postgres")
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresClinicRepository extends ClinicRepository {
    }

    /**
     * PostgreSQL clinic service offering repository bean.
     */
    @Requires(env = "postgres")
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresClinicServiceOfferingRepository extends ClinicServiceOfferingRepository {
    }

    /**
     * Postgres appointment repository used by the transaction-priority showcase.
     */
    @Requires(env = {"postgres"})
    @JdbcRepository(dialect = Dialect.POSTGRES)
    public interface PostgresAppointmentRepository extends AppointmentRepository {
        @NonNull
        @Override
        @Query(value = """
                SELECT a.* FROM "APPOINTMENTS" a WHERE a."id" = :appointmentId FOR UPDATE
                """, nativeQuery = true)
        Optional<Appointment> findByIdForUpdate(Integer appointmentId);

        @Override
        @Query(value = """
                SELECT a.* FROM "APPOINTMENTS" a WHERE a."STATUS" = 'AVAILABLE' ORDER BY a."DISPLAY_ORDER", a."id"
                """, nativeQuery = true)
        List<Appointment> findAvailableAppointments();
    }
}
