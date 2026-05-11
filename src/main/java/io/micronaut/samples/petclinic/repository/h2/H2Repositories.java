package io.micronaut.samples.petclinic.repository.h2;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.repository.GenericRepository;
import io.micronaut.samples.petclinic.model.*;
import io.micronaut.samples.petclinic.repository.*;
import java.util.Collection;
import java.util.List;

/**
 * H2-backed Micronaut Data repository beans used when no external database environment is active.
 */
public final class H2Repositories {
    private H2Repositories() {
    }

    /**
     * H2 owner repository bean.
     */
    @Requires(notEnv = {"mysql", "postgres", "oracle"})
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2OwnerRepository extends OwnerRepository {
    }

    /**
     * H2 pet repository bean.
     */
    @Requires(notEnv = {"mysql", "postgres", "oracle"})
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
    @Requires(notEnv = {"mysql", "postgres", "oracle"})
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2PetTypeRepository extends PetTypeRepository {
    }

    /**
     * H2 specialty repository bean.
     */
    @Requires(notEnv = {"mysql", "postgres", "oracle"})
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2SpecialtyRepository extends SpecialtyRepository {
    }

    /**
     * H2 vet repository bean.
     */
    @Requires(notEnv = {"mysql", "postgres", "oracle"})
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2VetRepository extends VetRepository {
        /**
         * Finds all vets using H2 SQL.
         *
         * @return all vets ordered by last name
         */
        @Override
        @Query(value = "SELECT v.* FROM VETS v ORDER BY v.LAST_NAME", nativeQuery = true)
        Collection<Vet> findAllWithSpecialties();
    }

    /**
     * H2 visit repository bean.
     */
    @Requires(notEnv = {"mysql", "postgres", "oracle"})
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
     * H2 vet-specialty join repository bean.
     */
    @Requires(notEnv = {"mysql", "postgres", "oracle"})
    @JdbcRepository(dialect = Dialect.H2)
    public interface H2VetSpecialtyRepository extends VetSpecialtyRepository {
        /**
         * Finds specialties for a vet using H2 SQL.
         *
         * @param vetId the vet id
         * @return specialties associated with the vet
         */
        @Override
        @Query(value = "SELECT s.* FROM SPECIALTIES s JOIN VET_SPECIALTIES vs ON vs.SPECIALTY_ID = s.id WHERE vs.VET_ID = :vetId ORDER BY s.NAME", nativeQuery = true)
        List<Specialty> findSpecialtiesByVetId(Integer vetId);
    }
}
