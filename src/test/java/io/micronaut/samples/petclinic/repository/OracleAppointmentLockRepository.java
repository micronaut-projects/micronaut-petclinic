package io.micronaut.samples.petclinic.repository;

import io.micronaut.context.annotation.Requires;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.GenericRepository;
import io.micronaut.samples.petclinic.model.Appointment;

/** Test-only probe for an appointment row held by another transaction. */
@JdbcRepository(dialect = Dialect.ORACLE)
@Requires(env = "oracle")
public interface OracleAppointmentLockRepository extends GenericRepository<Appointment, Integer> {
    @Query(value = "SELECT ID FROM APPOINTMENTS WHERE ID = :appointmentId FOR UPDATE NOWAIT", nativeQuery = true)
    Integer lockNowait(Integer appointmentId);
}
