package io.micronaut.samples.petclinic.repository.postgres;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.model.Pet;
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
                SELECT COUNT(*) FROM "VET_SPECIALTIES" WHERE "VET_ID" = :id
                """, nativeQuery = true)
        long countSpecialityLinks(Integer id);
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
        @Query(value = "SELECT a.* FROM APPOINTMENTS a WHERE a.ID = :appointmentId FOR UPDATE", nativeQuery = true)
        Optional<Appointment> findByIdForUpdate(Integer appointmentId);

        @Override
        @Query(value = "SELECT a.* FROM APPOINTMENTS a WHERE STATUS = 'AVAILABLE' ORDER BY DISPLAY_ORDER, ID", nativeQuery = true)
        List<Appointment> findAvailableAppointments();
    }
}
