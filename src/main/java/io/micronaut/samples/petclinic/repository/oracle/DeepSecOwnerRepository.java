package io.micronaut.samples.petclinic.repository.oracle;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.security.annotation.RunAs;

import java.util.Optional;

/**
 * Oracle-only repository methods used to demonstrate Deep Data Security
 * privilege elevation.
 *
 * <p>The ordinary owner queries remain in {@code OwnerRepository}. This
 * repository is deliberately separate so the elevated operation is easy to
 * identify in a demo and cannot accidentally become the default query path.</p>
 */
@Requires(env = "oracle-deepsec")
@JdbcRepository(dialect = Dialect.ORACLE)
public interface DeepSecOwnerRepository extends CrudRepository<Owner, Integer> {

    /**
     * Reads one owner's contact data with a role enabled only for this method.
     *
     * @param ownerId the owner id
     * @return the owner when the elevated operation can access it
     */
    @RunAs("ORACLE_DATA_ROLE_PETCLINIC_SUPPORT")
    @Query(value = "SELECT o.* FROM OWNERS o WHERE o.ID = :ownerId", nativeQuery = true)
    Optional<Owner> findByIdWithSupport(Integer ownerId);
}
