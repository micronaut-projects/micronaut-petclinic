package io.micronaut.samples.petclinic.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.JsonSubView;
import io.micronaut.data.annotation.JsonView;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.time.LocalDateTime;

import static io.micronaut.data.annotation.Relation.Kind.ONE_TO_MANY;

/**
 * Oracle JSON document representation of an owner and their pet-care history.
 * Micronaut Data generates the Oracle duality view from this mapping.
 *
 * @param id owner identifier exposed as the JSON document's {@code _id}
 * @param firstName owner's first name
 * @param lastName owner's last name
 * @param address owner's street address
 * @param city owner's city
 * @param telephone owner's telephone number
 * @param pets the owner's pets and their visits
 * @param metadata Oracle-managed duality document metadata
 */
@JsonView(value = "OWNER_CARE_DV", entity = Owner.class, operations = JsonView.Operation.UPDATE)
public record OwnerCareView(
        @Id
        @MappedProperty("ID")
        @JsonProperty("_id")
        Integer id,

        @NotBlank
        String firstName,

        @NotBlank
        String lastName,

        @NotBlank
        String address,

        @NotBlank
        String city,

        @Nullable
        String telephone,

        @Relation(ONE_TO_MANY)
        List<@Valid PetCareSubView> pets,

        @JsonProperty("_metadata")
        DualityMetadata metadata
) {
    public OwnerCareView {
        pets = pets == null ? List.of() : List.copyOf(pets);
    }

    /**
     * Nested pet document projected from the existing {@link Pet} table.
     *
     * @param id pet identifier
     * @param name pet name
     * @param birthDate pet birth date
     * @param visits the pet's visit history
     */
    @JsonSubView(entity = Pet.class, operations = JsonView.Operation.UPDATE)
    public record PetCareSubView(
            @Id Integer id,
            @NotBlank String name,
            @MappedProperty("BIRTH_DATE") LocalDateTime birthDate,
            @Relation(ONE_TO_MANY) List<@Valid VisitCareSubView> visits
    ) {
        public PetCareSubView {
            visits = visits == null ? List.of() : List.copyOf(visits);
        }
    }

    /**
     * Nested visit document projected from the existing {@link Visit} table.
     *
     * @param id visit identifier
     * @param date visit date
     * @param description visit description
     */
    @JsonSubView(entity = Visit.class, operations = JsonView.Operation.UPDATE)
    @Serdeable
    public record VisitCareSubView(
            @Id Integer id,
            @MappedProperty("VISIT_DATE") LocalDateTime date,
            @NotBlank String description
    ) {
    }

    /** Oracle duality view metadata used for optimistic concurrency control. */
    @Serdeable
    public record DualityMetadata(String etag, String asof) {
    }
}
