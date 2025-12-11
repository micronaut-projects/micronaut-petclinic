package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Entity representing a visit to the pet clinic.
 * A visit is associated with a pet and has a date and description.
 */
@Entity
@Table(name = "visits")
@Serdeable
public class Visit extends BaseEntity {

    @Column(name = "visit_date")
    @NotNull
    private LocalDate date;

    @Column(name = "description")
    @NotBlank
    private String description;

    @ManyToOne
    @JoinColumn(name = "pet_id")
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
    public Integer getPetId() {
        return this.pet != null ? this.pet.getId() : null;
    }

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
