package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Transient;
import static io.micronaut.data.annotation.Relation.Kind.MANY_TO_ONE;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Entity representing a visit to the pet clinic.
 * A visit is associated with a pet and has a date and description.
 */
@MappedEntity("visits")
@Serdeable
public class Visit extends BaseEntity {

    @MappedProperty("visit_date")
    @NotNull
    private LocalDate date;

    @MappedProperty("description")
    @NotBlank
    private String description;

    @Relation(MANY_TO_ONE)
    @MappedProperty("pet_id")
    private Pet pet;

    public Visit() {
        this.date = LocalDate.now();
    }

    public LocalDate getDate() {
        return this.date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Pet getPet() {
        return this.pet;
    }

    public void setPet(Pet pet) {
        this.pet = pet;
    }

    /**
     * Get the pet's ID for this visit.
     * @return the pet ID, or null if no pet is associated
     */
    @Transient
    public Integer getPetId() {
        return this.pet != null ? this.pet.getId() : null;
    }

    // pet_id is managed via the Pet association

    @Override
    public String toString() {
        return "Visit{" +
                "id=" + getId() +
                ", date=" + date +
                ", description='" + description + '\'' +
                ", petId=" + getPetId() +
                '}';
    }
}
