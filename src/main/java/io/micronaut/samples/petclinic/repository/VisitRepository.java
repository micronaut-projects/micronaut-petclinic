package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.annotation.Join;
import io.micronaut.data.model.Sort;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.Visit;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;

import static io.micronaut.data.annotation.Join.Type.LEFT_FETCH;

/**
 * Repository for {@link Visit} entities.
 * Uses Micronaut Data JDBC for compile-time query generation.
 * Dialect-specific {@code @JdbcRepository} beans extend this interface.
 */
public interface VisitRepository extends CrudRepository<Visit, Integer> {

    /**
     * Finds all visits with their pet and owner loaded for display and filtering.
     *
     * @param sort the result ordering
     * @return visits with related pet and owner data
     */
    @Join(value = "pet", type = LEFT_FETCH)
    @Join(value = "pet.type", type = LEFT_FETCH)
    @Join(value = "pet.owner", type = LEFT_FETCH)
    List<Visit> findAll(Sort sort);

    /**
     * Find all visits for a specific pet.
     * @param petId the pet ID
     * @return collection of visits for the pet
     */
    Collection<Visit> findByPetId(Integer petId);

    /**
     * Find all visits for a specific pet.
     * @param petId the pet ID
     * @return collection of visits for the pet
     */
    Collection<Visit> findByPetName(String perName);


    /**
     * Finds visits for any of the supplied pets.
     *
     * @param petIds the pet IDs
     * @return matching visits
     */
    Collection<Visit> findByPetIdIn(List<Integer> petIds);

    /**
     * Finds visits containing text in their description.
     *
     * @param description the description fragment
     * @return matching visits
     */
    Collection<Visit> findByDescriptionContainingIgnoreCase(String description);

    /**
     * Finds visits on or after a date.
     *
     * @param date the inclusive lower date bound
     * @return matching visits
     */
    Collection<Visit> findByDateGreaterThanEqual(LocalDate date);

    /**
     * Finds visits on or before a date.
     *
     * @param date the inclusive upper date bound
     * @return matching visits
     */
    Collection<Visit> findByDateLessThanEqual(LocalDate date);

    /**
     * Finds visits whose duration is longer than the supplied duration.
     *
     * @param duration the minimum duration, exclusive
     * @return matching visits
     */
    Collection<Visit> findByDurationGreaterThan(Duration duration);

    /**
     * Finds visits whose duration is at least the supplied duration.
     *
     * @param duration the inclusive minimum duration
     * @return matching visits
     */
    Collection<Visit> findByDurationGreaterThanEqual(Duration duration);

    /**
     * Finds visits whose duration is at most the supplied duration.
     *
     * @param duration the inclusive maximum duration
     * @return matching visits
     */
    Collection<Visit> findByDurationLessThanEqual(Duration duration);

    /**
     * Finds visits whose recommended follow-up period is longer than the supplied period.
     * The period should contain only years and months for Oracle interval support.
     *
     * @param period the minimum follow-up period, exclusive
     * @return matching visits
     */
    Collection<Visit> findByPeriodGreaterThan(Period period);

    /**
     * Finds visits whose follow-up period is at least the supplied period.
     *
     * @param period the inclusive minimum period
     * @return matching visits
     */
    Collection<Visit> findByPeriodGreaterThanEqual(Period period);

    /**
     * Finds visits whose follow-up period is at most the supplied period.
     *
     * @param period the inclusive maximum period
     * @return matching visits
     */
    Collection<Visit> findByPeriodLessThanEqual(Period period);

    /**
     * Finds visits whose follow-up date/duration/period meet the criteria.
     *
     * @param from start date.
     * @param to end date.
     * @param duration visit duration.
     * @param period visit period.
     * @return List of visits that match the criteria.
     */
    @Join(value = "pet", type = LEFT_FETCH)
    @Join(value = "pet.type", type = LEFT_FETCH)
    @Join(value = "pet.owner", type = LEFT_FETCH)
    List<Visit> findByDateBetweenAndDurationLessThanEqualsAndPeriodLessThanEquals(@NonNull LocalDate from, @NonNull LocalDate to, @NonNull Duration duration, @NonNull Period period);
}
