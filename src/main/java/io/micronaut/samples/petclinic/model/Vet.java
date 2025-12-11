package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;
import java.util.*;

/**
 * Entity representing a veterinarian.
 * A vet can have multiple specialties.
 */
@Entity
@Table(name = "vets")
@Serdeable
public class Vet extends Person {

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "vet_specialties",
            joinColumns = @JoinColumn(name = "vet_id"),
            inverseJoinColumns = @JoinColumn(name = "specialty_id"))
    @OrderBy("name ASC")
    private Set<Specialty> specialties = new LinkedHashSet<>();

    /**
     * Get the vet's specialties, sorted by name.
     * @return unmodifiable list of specialties
     */
    public List<Specialty> getSpecialties() {
        List<Specialty> sortedSpecialties = new ArrayList<>(this.specialties);
        sortedSpecialties.sort(Comparator.comparing(Specialty::getName));
        return Collections.unmodifiableList(sortedSpecialties);
    }

    /**
     * Get the number of specialties for this vet.
     * @return the number of specialties
     */
    public int getNrOfSpecialties() {
        return this.specialties.size();
    }

    /**
     * Add a specialty to this vet.
     * @param specialty the specialty to add
     */
    public void addSpecialty(Specialty specialty) {
        this.specialties.add(specialty);
    }

    /**
     * Remove a specialty from this vet.
     * @param specialty the specialty to remove
     */
    public void removeSpecialty(Specialty specialty) {
        this.specialties.remove(specialty);
    }

    /**
     * Get a comma-separated list of specialty names.
     * @return specialty names, or "none" if no specialties
     */
    public String getSpecialtiesAsString() {
        if (this.specialties.isEmpty()) {
            return "none";
        }
        List<String> names = new ArrayList<>();
        for (Specialty specialty : getSpecialties()) {
            names.add(specialty.getName());
        }
        return String.join(", ", names);
    }

    @Override
    public String toString() {
        return "Vet{" +
                "id=" + getId() +
                ", firstName='" + getFirstName() + '\'' +
                ", lastName='" + getLastName() + '\'' +
                ", specialties=" + getSpecialtiesAsString() +
                '}';
    }
}
