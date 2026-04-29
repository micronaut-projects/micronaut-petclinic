package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.GenericRepository;
import io.micronaut.samples.petclinic.model.Specialty;
import io.micronaut.samples.petclinic.model.VetSpecialty;
import java.util.List;

@JdbcRepository(dialect = Dialect.ANSI)
public interface VetSpecialtyRepository extends GenericRepository<VetSpecialty, VetSpecialty> {

    void save(VetSpecialty entity);

    @Query("SELECT s.* FROM specialties s JOIN vet_specialties vs ON vs.specialty_id = s.id WHERE vs.vet_id = :vetId ORDER BY s.name")
    List<Specialty> findSpecialtiesByVetId(Integer vetId);
}
