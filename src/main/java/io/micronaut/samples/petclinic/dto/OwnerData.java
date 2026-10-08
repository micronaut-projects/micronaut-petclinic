package io.micronaut.samples.petclinic.dto;

import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.Objects;

/**
 * A non-cyclic representation of an owner suitable for the demo API.
 * A protected Oracle column remains visible as {@code null}, making column
 * masking obvious without serializing the complete entity graph.
 *
 * @param id owner id
 * @param firstName owner first name
 * @param lastName owner last name
 * @param city owner city
 * @param address owner address
 * @param telephone owner telephone, possibly masked by Oracle
 * @param pets names of pets returned by the same secured query graph
 */
@Serdeable
public record OwnerData(Integer id,
                        String firstName,
                        String lastName,
                        String city,
                        String address,
                        String telephone,
                        List<String> pets) {

    public static OwnerData from(Owner owner) {
        List<String> petNames = owner.pets() == null ? List.of() : owner.pets().stream()
                .map(pet -> pet != null ? pet.name() : null)
                .filter(Objects::nonNull)
                .toList();
        return new OwnerData(owner.id(), owner.firstName(), owner.lastName(), owner.city(), owner.address(), owner.telephone(), petNames);
    }
}