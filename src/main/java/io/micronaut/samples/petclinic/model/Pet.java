package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Transient;
import static io.micronaut.data.annotation.Relation.Kind.MANY_TO_ONE;
import static io.micronaut.data.annotation.Relation.Kind.ONE_TO_MANY;
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

    @Relation(MANY_TO_ONE)
    @MappedProperty("type_id")
    @NotNull
    private PetType type;

    @Relation(MANY_TO_ONE)
    @MappedProperty("owner_id")
    private Owner owner;

    @Relation(value = ONE_TO_MANY, mappedBy = "pet")
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


    @Transient
    public Integer getTypeId() {
        return this.type != null ? this.type.getId() : null;
    }

    public Owner getOwner() {
        return this.owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }


    @Transient
    public Integer getOwnerId() {
        return this.owner != null ? this.owner.getId() : null;
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
