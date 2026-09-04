package io.micronaut.samples.petclinic.model;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Builder;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;

import static io.micronaut.data.annotation.Relation.Kind.MANY_TO_ONE;

/**
 * Entity representing a visit to the pet clinic.
 * A visit is associated with a pet and has a date and description.
 *
 * @param id the database identifier, or {@code null} for a new visit
 * @param date the visit date
 * @param description the visit description
 * @param pet the pet that received the visit
 * @param duration the length of the visit
 * @param period the recommended follow-up period
 */
@MappedEntity("VISITS")
@Serdeable
@Builder
@Wither
public record Visit(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("VISIT_DATE")
        @NotNull
        LocalDate date,

        @MappedProperty("DESCRIPTION")
        @NotBlank
        String description,

        @Relation(MANY_TO_ONE)
        @MappedProperty("PET_ID")
        Pet pet,

        @MappedProperty(value = "DURATION")
        Duration duration,

        @MappedProperty(value = "FOLLOW_UP_PERIOD")
        Period period
) implements BaseEntity, VisitWither {

    /**
     * Creates a visit.
     *
     * @param id the database identifier, or {@code null} for a new visit
     * @param date the visit date
     * @param description the visit description
     * @param pet the pet that received the visit
     * @param duration the length of the visit
     * @param period the recommended follow-up period
     */
    public Visit(Integer id, LocalDate date, String description, @Nullable Pet pet, Duration duration, Period period) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.pet = pet;
        this.duration = duration;
        this.period = period;
    }

    /**
     * Creates an empty visit for framework binding.
     */
    public Visit() {
        this(null, LocalDate.now(), null, null, Duration.ZERO, Period.ZERO);
    }

    /**
     * Creates a visit with a duration and no follow-up period.
     *
     * @param id the database identifier, or {@code null} for a new visit
     * @param date the visit date
     * @param description the visit description
     * @param pet the pet that received the visit
     * @param duration the length of the visit
     */
    public Visit(Integer id, LocalDate date, String description, @Nullable Pet pet, Duration duration) {
        this(id, date, description, pet, duration, Period.ZERO);
    }

    /**
     * Creates a new visit without an id with no follow-up period.
     *
     * @param date the visit date
     * @param description the visit description
     * @param pet the pet that received the visit
     * @param duration the length of the visit
     */
    public Visit(LocalDate date, String description, Pet pet, Duration duration) {
        this(null, date, description, pet, duration, Period.ZERO);
    }

    /**
     * Creates a new visit without an id.
     *
     * @param date the visit date
     * @param description the visit description
     * @param pet the pet that received the visit
     * @param duration the length of the visit
     * @param period the recommended follow-up period
     */
    public Visit(LocalDate date, String description, Pet pet, Duration duration, Period period) {
        this(null, date, description, pet, duration, period);
    }

    /**
     * Creates a new visit without an id using zero values for the interval fields.
     *
     * @param date the visit date
     * @param description the visit description
     * @param pet the pet that received the visit
     */
    public Visit(LocalDate date, String description, Pet pet) {
        this(null, date, description, pet, Duration.ZERO, Period.ZERO);
    }

    /**
     * Compares visits by entity identity.
     *
     * @param other the object being compared
     * @return {@code true} when the other object represents the same visit
     */
    @Override
    public boolean equals(Object other) {
        return BaseEntity.entityEquals(this, other);
    }

    /**
     * Returns the entity-identity hash code.
     *
     * @return the hash code based on the visit id
     */
    @Override
    public int hashCode() {
        return BaseEntity.entityHashCode(this);
    }

    /**
     * Returns a diagnostic representation of the visit.
     *
     * @return a string containing the visit fields
     */
    @Override
    public String toString() {
        return "Visit{" +
                "id=" + id +
                ", date=" + date +
                ", description='" + description + '\'' +
                ", petId=" + (pet != null ? pet.id() : null) +
                '}';
    }
}
