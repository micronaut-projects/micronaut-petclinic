package io.micronaut.samples.petclinic.model;

import io.micronaut.core.annotation.Creator;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Relation;
import io.micronaut.data.annotation.Transient;
import io.micronaut.data.annotation.sql.ETaggable;
import io.micronaut.data.annotation.sql.GeneratedETag;
import io.micronaut.data.annotation.sql.JoinColumn;
import io.micronaut.data.annotation.sql.JoinTable;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.sourcegen.annotations.Wither;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entity representing a veterinarian.
 * A vet can have multiple specialities.
 *
 * @param id the database identifier, or {@code null} for a new vet
 * @param firstName the vet's first name
 * @param lastName the vet's last name
 * @param specialities the specialities associated with the vet
 * @param specialitiesHash the hash of its sorted persisted speciality IDs
 * @param etag the generated optimistic-locking value
 */
@MappedEntity("VETS")
@Serdeable
@Wither
@ETaggable
public record Vet(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("FIRST_NAME")
        @NotBlank
        String firstName,

        @MappedProperty("LAST_NAME")
        @NotBlank
        String lastName,

        @Relation(value = Relation.Kind.MANY_TO_MANY, cascade = Relation.Cascade.PERSIST)
        @JoinTable(
                name = "VET_SPECIALTIES",
                joinColumns = @JoinColumn(name = "VET_ID", referencedColumnName = "id"),
                inverseJoinColumns = @JoinColumn(name = "SPECIALTY_ID", referencedColumnName = "id")
        )
        @NonNull
        Set<Speciality> specialities,

        @MappedProperty("SPECIALITIES_ETAG_HASH")
        @NonNull String specialitiesHash,

        @Nullable
        // Explicit for non-Oracle profiles, which define this function; Oracle already uses it by default.
        // The emulated function signatures are positional and must match Vet's ETag inputs exactly:
        // id, firstName, lastName, specialitiesHash. Changing persisted ETag inputs, including @ETagValue exclusions,
        // makes runtime function lookup fail.
        @GeneratedETag(function = "SYS_ROW_ETAG")
        String etag
) implements Person, VetWither {

    /**
     * Creates an immutable veterinarian with an independent speciality set.
     *
     * @param id veterinarian identifier, or {@code null} when creating
     * @param firstName veterinarian first name
     * @param lastName veterinarian last name
     * @param specialities speciality references; omitted selections become an empty set
     * @param specialitiesHash persisted hash; it is recomputed from the loaded speciality IDs
     * @param etag the loaded ETag, or {@code null} for a new veterinarian
     */
    @Creator
    public Vet {
        specialities = specialities == null ? Set.of() : Set.copyOf(specialities);
        // The ETag input must include every link, so compute it from fully loaded specialities with persisted IDs.
        specialitiesHash = hashSpecialityIds(specialities);
    }

    /**
     * Creates a veterinarian and computes its speciality hash from the assigned IDs.
     *
     * @param id veterinarian identifier
     * @param firstName veterinarian first name
     * @param lastName veterinarian last name
     * @param specialities speciality references
     * @param etag current ETag, or {@code null} for a new veterinarian
     */
    public Vet(Integer id, String firstName, String lastName, Set<Speciality> specialities, String etag) {
        this(id, firstName, lastName, specialities, hashSpecialityIds(specialities), etag);
    }

    /**
     * Creates an empty vet for framework binding.
     */
    public Vet() {
        this(null, null, null, Set.of(), hashSpecialityIds(Set.of()), null);
    }

    /**
     * Creates a new vet without an id or specialities.
     *
     * @param firstName the vet's first name
     * @param lastName the vet's last name
     */
    public Vet(String firstName, String lastName) {
        this(null, firstName, lastName, Set.of(), hashSpecialityIds(Set.of()), null);
    }

    private static String hashSpecialityIds(Set<Speciality> specialities) {
        Set<Speciality> loadedSpecialities = specialities == null ? Set.of() : specialities;
        String sortedIds = loadedSpecialities.stream()
                .map(speciality -> {
                    Integer id = speciality.id();
                    if (id == null) {
                        throw new IllegalStateException("ETag hashing requires specialities with persisted IDs.");
                    }
                    return id;
                })
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(sortedIds.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toUpperCase(java.util.Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    /**
     * Returns the vet specialities sorted by name, with unresolved names first.
     *
     * @return an immutable sorted speciality set
     */
    public Set<Speciality> specialities() {
        List<Speciality> sortedSpecialities = new ArrayList<>(this.specialities);
        sortedSpecialities.sort(Comparator.comparing(Speciality::name, Comparator.nullsFirst(Comparator.naturalOrder())));
        return Collections.unmodifiableSet(new LinkedHashSet<>(sortedSpecialities));
    }

    /**
     * Returns a comma-separated list of speciality names for display.
     *
     * @return the speciality names, or {@code none} when the vet has no specialities
     */
    @Transient
    public String getSpecialitiesAsString() {
        if (specialities.isEmpty()) {
            return "none";
        }
        List<String> names = new ArrayList<>();
        for (Speciality speciality : specialities()) {
            names.add(speciality.name());
        }
        return String.join(", ", names);
    }

    /**
     * Compares vets by entity identity.
     *
     * @param other the object being compared
     * @return {@code true} when the other object represents the same vet
     */
    @Override
    public boolean equals(Object other) {
        return BaseEntity.entityEquals(this, other);
    }

    /**
     * Returns the entity-identity hash code.
     *
     * @return the hash code based on the vet id
     */
    @Override
    public int hashCode() {
        return BaseEntity.entityHashCode(this);
    }

    /**
     * Returns a diagnostic representation of the vet.
     *
     * @return a string containing the vet fields
     */
    @Override
    public String toString() {
        return "Vet{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", specialities=" + getSpecialitiesAsString() +
                '}';
    }
}
