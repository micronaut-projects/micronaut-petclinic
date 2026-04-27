package io.micronaut.samples.petclinic.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.Transient;
import java.util.Objects;

/**
 * Base entity class providing common id property and behavior.
 * All Pet Clinic domain entities extend this class.
 */
@MappedEntity
@Serdeable
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    private Integer id;

    // Avoid schema generation trying to persist derived boolean property.
    @Transient
    public boolean isNew() {
        return this.id == null;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Check if this entity is new (not yet persisted).
     * @return true if the entity has not been persisted yet
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
