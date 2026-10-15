package io.micronaut.samples.petclinic.repository;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for interval queries on {@link VisitRepository}.
 */
@MicronautTest
@Requires(env= "oracle")
class VisitRepositoryTest {

    @Inject
    VisitRepository visitRepository;

    @Test
    void shouldReadIntervalValues() {
        Collection<Visit> visits = visitRepository.findByPetName("Samantha");

        assertThat(visits).extracting(Visit::duration)
                .containsExactly(Duration.ofHours(1), Duration.ofMinutes(30));
        assertThat(visits).extracting(Visit::period)
                .containsExactly(Period.ofMonths(12), Period.ofMonths(6));
    }

    @Test
    void shouldFindVisitsGreaterThanDuration() {
        Collection<Visit> visits = visitRepository.findByDurationGreaterThan(Duration.ofMinutes(180));

        assertThat(visits).isNotEmpty()
                .allSatisfy(visit -> assertThat(visit.duration()).isGreaterThan(Duration.ofMinutes(180)));
    }

    @Test
    void shouldFindVisitsGreaterThanOrEqualToDuration() {
        Collection<Visit> visits = visitRepository.findByDurationGreaterThanEqual(Duration.ofMinutes(210));

        assertThat(visits).isNotEmpty()
                .anySatisfy(visit -> assertThat(visit.duration()).isEqualTo(Duration.ofMinutes(210)))
                .allSatisfy(visit -> assertThat(visit.duration()).isGreaterThanOrEqualTo(Duration.ofMinutes(210)));
    }

    @Test
    void shouldFindVisitsLessThanOrEqualToDuration() {
        Collection<Visit> visits = visitRepository.findByDurationLessThanEqual(Duration.ofMinutes(45));

        assertThat(visits).isNotEmpty()
                .anySatisfy(visit -> assertThat(visit.duration()).isEqualTo(Duration.ofMinutes(45)))
                .allSatisfy(visit -> assertThat(visit.duration()).isLessThanOrEqualTo(Duration.ofMinutes(45)));
    }

    @Test
    void shouldFindVisitsGreaterThanFollowUpPeriod() {
        Collection<Visit> visits = visitRepository.findByPeriodGreaterThan(Period.ofMonths(24));

        assertThat(visits).isNotEmpty()
                .allSatisfy(visit -> assertThat(visit.period().toTotalMonths()).isGreaterThan(24));
    }

    @Test
    void shouldFindVisitsGreaterThanOrEqualToFollowUpPeriod() {
        Collection<Visit> visits = visitRepository.findByPeriodGreaterThanEqual(Period.ofMonths(24));

        assertThat(visits).isNotEmpty()
                .anySatisfy(visit -> assertThat(visit.period()).isEqualTo(Period.ofMonths(24)))
                .allSatisfy(visit -> assertThat(visit.period().toTotalMonths()).isGreaterThanOrEqualTo(24));
    }

    @Test
    void shouldFindVisitsLessThanOrEqualToFollowUpPeriod() {
        Collection<Visit> visits = visitRepository.findByPeriodLessThanEqual(Period.ofMonths(6));

        assertThat(visits).isNotEmpty()
                .anySatisfy(visit -> assertThat(visit.period()).isEqualTo(Period.ofMonths(6)))
                .allSatisfy(visit -> assertThat(visit.period().toTotalMonths()).isLessThanOrEqualTo(6));
    }

    @Test
    void shouldFindVisitsWithinDateAndIntervalBounds() {
        List<Visit> visits = visitRepository.findByDateBetweenAndDurationLessThanEqualsAndPeriodLessThanEquals(
                LocalDate.of(2017, 1, 1),
                LocalDate.of(2020, 12, 31),
                Duration.ofMinutes(90),
                Period.ofMonths(18)
        );

        assertThat(visits).hasSize(4)
                .allSatisfy(visit -> {
                    assertThat(visit.date()).isBetween(LocalDate.of(2017, 1, 1),
                            LocalDate.of(2020, 12, 31));
                    assertThat(visit.duration()).isLessThanOrEqualTo(Duration.ofMinutes(90));
                    assertThat(visit.period().toTotalMonths()).isLessThanOrEqualTo(18);
                });
    }
}
