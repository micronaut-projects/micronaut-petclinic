package io.micronaut.samples.petclinic.repository.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.samples.petclinic.model.Appointment;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.repository.AppointmentRepository;
import io.micronaut.samples.petclinic.repository.ClinicRepository;
import io.micronaut.samples.petclinic.repository.ClinicServiceOfferingRepository;
import io.micronaut.samples.petclinic.repository.OwnerRepository;
import io.micronaut.samples.petclinic.repository.PetCareChunkRepository;
import io.micronaut.samples.petclinic.repository.PetCareDocumentRepository;
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
 * Oracle-backed Micronaut Data repository beans active in the {@code oracle}
 * and {@code oracle-deepsec} environments.
 */
public final class OracleRepositories {
    private OracleRepositories() {
    }

    /**
     * Oracle owner repository bean.
     */
    @Requires(env = {"oracle", "oracle-deepsec"})
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleOwnerRepository extends OwnerRepository {
    }

    /**
     * Oracle clinic repository bean.
     */
    @Requires(env = {"oracle", "oracle-deepsec"})
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleClinicRepository extends ClinicRepository {
    }

    /**
     * Oracle clinic service offering repository bean.
     */
    @Requires(env = "oracle")
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleClinicServiceOfferingRepository extends ClinicServiceOfferingRepository {
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
    @Requires(env = {"oracle", "oracle-deepsec"})
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
    @Requires(env = {"oracle", "oracle-deepsec"})
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OraclePetTypeRepository extends PetTypeRepository {
    }

    /**
     * Oracle speciality repository bean.
     */
    @Requires(env = {"oracle", "oracle-deepsec"})
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleSpecialityRepository extends SpecialityRepository {
    }

    /**
     * Oracle vet repository bean.
     */
    @Requires(env = {"oracle", "oracle-deepsec"})
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleVetRepository extends VetRepository {
    }

    /**
     * Oracle visit repository bean.
     */
    @Requires(env = {"oracle", "oracle-deepsec"})
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleVisitRepository extends VisitRepository {
    }

    /**
     * Oracle appointment repository used by the transaction-priority showcase.
     */
    @Requires(env = "oracle")
    @JdbcRepository(dialect = Dialect.ORACLE)
    public interface OracleAppointmentRepository extends AppointmentRepository {

        /** Waits up to ten seconds to acquire the row lock, not to hold it. */
        @NonNull
        @Override
        @Query(value = "SELECT a.* FROM APPOINTMENTS a WHERE a.ID = :appointmentId FOR UPDATE WAIT 10", nativeQuery = true)
        Optional<Appointment> findByIdForUpdate(Integer appointmentId);

        @Override
        @Query(value = "SELECT a.* FROM APPOINTMENTS a WHERE STATUS = 'AVAILABLE' ORDER BY DISPLAY_ORDER, ID", nativeQuery = true)
        List<Appointment> findAvailableAppointments();
    }

}
