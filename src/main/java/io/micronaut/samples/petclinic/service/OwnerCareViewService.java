package io.micronaut.samples.petclinic.service;

import io.micronaut.context.annotation.Requires;
import io.micronaut.samples.petclinic.model.OwnerCareView;
import io.micronaut.samples.petclinic.model.OwnerCareView.PetCareSubView;
import io.micronaut.samples.petclinic.model.OwnerCareView.VisitCareSubView;
import io.micronaut.samples.petclinic.repository.OwnerCareViewRepository;
import jakarta.inject.Singleton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Loads and updates owner-care documents through Micronaut Data's JSON view
 * repository while protecting their shape and Oracle ETAG.
 */
@Singleton
@Requires(env = "oracle")
public class OwnerCareViewService {

    private final OwnerCareViewRepository repository;

    public OwnerCareViewService(OwnerCareViewRepository repository) {
        this.repository = repository;
    }

    public OwnerCareView findById(Integer ownerId) {
        return repository.findById(ownerId).orElse(null);
    }

    /**
     * Updates an existing document while preserving nested membership and IDs.
     *
     * @param ownerId path owner identifier
     * @param updated edited document
     * @return the persisted document with a fresh ETAG
     */
    public OwnerCareView update(Integer ownerId, OwnerCareView updated) {
        OwnerCareView current = repository.findById(ownerId)
                .orElseThrow(() -> new OwnerCareNotFoundException(ownerId));
        validateDocument(ownerId, current, updated);
        repository.update(updated);
        return repository.findById(ownerId).orElseThrow(() -> new OwnerCareNotFoundException(ownerId));
    }

    private static void validateDocument(Integer ownerId, OwnerCareView current, OwnerCareView updated) {
        if (updated == null || !Objects.equals(ownerId, updated.id())) {
            throw new IllegalArgumentException("The document _id must match the owner in the URL.");
        }
        if (updated.metadata() == null || updated.metadata().etag() == null || updated.metadata().etag().isBlank()) {
            throw new IllegalArgumentException("The document must include its _metadata.etag value.");
        }
        if (!samePetIds(current, updated)) {
            throw new IllegalArgumentException("The pets in an owner document cannot be added, removed, or reassigned here.");
        }
        if (!sameVisitIdsByPet(current, updated)) {
            throw new IllegalArgumentException("Visits in an owner document cannot be added, removed, or reassigned here.");
        }
        if (isBlank(updated.firstName()) || isBlank(updated.lastName())
                || isBlank(updated.address()) || isBlank(updated.city())) {
            throw new IllegalArgumentException("Owner name, address, and city are required.");
        }
        if (updated.telephone() != null && !updated.telephone().matches("[0-9]{1,10}")) {
            throw new IllegalArgumentException("The telephone number must contain up to 10 digits.");
        }
        for (PetCareSubView pet : updated.pets()) {
            if (isBlank(pet.name()) || pet.birthDate() == null) {
                throw new IllegalArgumentException("Each pet needs a name and birth date.");
            }
            for (VisitCareSubView visit : pet.visits()) {
                if (visit.date() == null || isBlank(visit.description())) {
                    throw new IllegalArgumentException("Each visit needs a date and description.");
                }
            }
        }
    }

    private static boolean samePetIds(OwnerCareView first, OwnerCareView second) {
        Set<Integer> firstIds = new HashSet<>();
        Set<Integer> secondIds = new HashSet<>();
        first.pets().forEach(pet -> firstIds.add(pet.id()));
        second.pets().forEach(pet -> secondIds.add(pet.id()));
        return firstIds.size() == first.pets().size()
                && secondIds.size() == second.pets().size()
                && firstIds.equals(secondIds);
    }

    private static boolean sameVisitIdsByPet(OwnerCareView first, OwnerCareView second) {
        Map<Integer, Set<Integer>> firstVisits = visitIdsByPet(first);
        Map<Integer, Set<Integer>> secondVisits = visitIdsByPet(second);
        if (firstVisits == null || secondVisits == null) {
            return false;
        }
        return firstVisits.equals(secondVisits);
    }

    private static Map<Integer, Set<Integer>> visitIdsByPet(OwnerCareView document) {
        Map<Integer, Set<Integer>> result = new HashMap<>();
        for (PetCareSubView pet : document.pets()) {
            Set<Integer> visitIds = new HashSet<>();
            for (VisitCareSubView visit : pet.visits()) {
                visitIds.add(visit.id());
            }
            if (visitIds.size() != pet.visits().size() || result.put(pet.id(), visitIds) != null) {
                return null;
            }
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Thrown when an owner document does not exist. */
    public static final class OwnerCareNotFoundException extends RuntimeException {
        public OwnerCareNotFoundException(Integer ownerId) {
            super("Owner " + ownerId + " was not found.");
        }
    }
}
