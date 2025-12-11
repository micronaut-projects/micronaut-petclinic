package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Entity representing a pet.
 * A pet belongs to an owner and has a type.
 */
@Entity
@Table(name = "pets")
@Serdeable
public class Pet extends NamedEntity {

    @Column(name = "birth_date")
    @NotNull
    private LocalDate birthDate;

    @ManyToOne
    @JoinColumn(name = "type_id")
    @NotNull
    private PetType type;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "pet", fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderBy("date ASC")
    private List<Visit> visits = new ArrayList<>();

    public LocalDate getBirthDate() {
        return this.birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public PetType getType() {
        return this.type;
    }

    public void setType(PetType type) {
        this.type = type;
    }

    public Owner getOwner() {
        return this.owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Get the visits for this pet, sorted by date.
     * @return unmodifiable list of visits
     */
    public List<Visit> getVisits() {
        List<Visit> sortedVisits = new ArrayList<>(this.visits);
        sortedVisits.sort(Comparator.comparing(Visit::getDate));
        return Collections.unmodifiableList(sortedVisits);
    }

    /**
     * Add a visit to this pet.
     * @param visit the visit to add
     */
    public void addVisit(Visit visit) {
        this.visits.add(visit);
        visit.setPet(this);
    }

    @Override
    public String toString() {
        return "Pet{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                ", birthDate=" + birthDate +
                ", type=" + (type != null ? type.getName() : null) +
                '}';
    }
}
