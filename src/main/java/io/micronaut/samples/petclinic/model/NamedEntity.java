package io.micronaut.samples.petclinic.model;

/**
 * Shared contract for entities with a name.
 */
public sealed interface NamedEntity extends BaseEntity permits Pet, PetType, Specialty {

    /**
     * Returns the display name.
     *
     * @return the entity name
     */
    String name();

}
