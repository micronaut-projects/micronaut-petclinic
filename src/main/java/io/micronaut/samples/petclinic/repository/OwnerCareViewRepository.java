package io.micronaut.samples.petclinic.repository;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.OwnerCareView;

/**
 * Micronaut Data repository for the generated Oracle owner-care duality view.
 */
@Requires(env = "oracle")
@JdbcRepository(dialect = Dialect.ORACLE)
public interface OwnerCareViewRepository extends CrudRepository<OwnerCareView, Integer> {
}
