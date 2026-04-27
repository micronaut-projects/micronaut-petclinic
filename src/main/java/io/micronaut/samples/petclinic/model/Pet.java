package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
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
@MappedEntity("pets")
@Serdeable
public class Pet extends NamedEntity {

    @MappedProperty("birth_date")
    @NotNull
    private LocalDate birthDate;

    @MappedProperty("type_id")
    @NotNull
    private Integer typeId;

    @MappedProperty("owner_id")
    private Integer ownerId;

    private PetType type;
    private Owner owner;
    private List<Visit> visits = new ArrayList<>();

    public LocalDate getBirthDate() {
        return this.birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    @io.micronaut.data.annotation.Transient
    public PetType getType() {
        return this.type;
    }

    public void setType(PetType type) {
        this.type = type;
    }


    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    @io.micronaut.data.annotation.Transient
    public Owner getOwner() {
        return this.owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }


    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Get the visits for this pet, sorted by date.
     * @return unmodifiable list of visits
     */
    @io.micronaut.data.annotation.Transient
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

    @io.micronaut.data.annotation.Transient
    public void setVisits(List<Visit> visits) {
        this.visits = visits != null ? visits : new ArrayList<>();
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
