package io.micronaut.samples.petclinic.service;

import io.micronaut.data.exceptions.OptimisticLockException;
import io.micronaut.samples.petclinic.dto.SpecialityDto;
import io.micronaut.samples.petclinic.dto.VetForm;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.exception.PreconditionFailedException;
import io.micronaut.samples.petclinic.exception.PreconditionRequiredException;
import io.micronaut.samples.petclinic.model.Speciality;
import io.micronaut.samples.petclinic.model.Vet;
import io.micronaut.samples.petclinic.repository.SpecialityRepository;
import io.micronaut.samples.petclinic.repository.VetRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Manages veterinarians, speciality assignments, and ETag-checked mutations.
 */
@Singleton
public class VetService {

    private final VetRepository vetRepository;
    private final SpecialityRepository specialityRepository;

    /**
     * Creates the service for veterinarian and speciality operations.
     *
     * @param vetRepository veterinarian persistence operations
     * @param specialityRepository speciality persistence operations
     */
    public VetService(VetRepository vetRepository, SpecialityRepository specialityRepository) {
        this.vetRepository = vetRepository;
        this.specialityRepository = specialityRepository;
    }

    /**
     * Finds all veterinarians and their fetched specialities.
     *
     * @return veterinarians ordered by last name
     */
    public Collection<Vet> findAll() {
        return vetRepository.findAll();
    }

    /**
     * Find a vet by ID.
     * @param id the vet ID
     * @return the veterinarian with its ETag and specialities
     * @throws NotFoundEntityException if the identifier does not exist
     */
    public Vet findById(Integer id) throws NotFoundEntityException {
        Vet vet = vetRepository.findById(id).orElseThrow(NotFoundEntityException::new);
        Objects.requireNonNull(vet.etag());
        return vet;
    }

    /**
     * Lists specialities available for assignment to a vet.
     *
     * @return specialities ordered by name
     */
    public List<Speciality> findSpecialities() {
        return specialityRepository.findAllOrderByName();
    }

    /**
     * Finds a speciality by its identifier.
     *
     * @param id speciality identifier
     * @return the persisted speciality
     * @throws NotFoundEntityException if the identifier does not exist
     */
    public Speciality findSpecialityById(Integer id) throws NotFoundEntityException {
        return specialityRepository.findById(id).orElseThrow(NotFoundEntityException::new);
    }

    /**
     * Finds a specialities by their identifiers.
     *
     * @param  ids specialities identifiers
     * @return Set of the persisted specialities
     * @throws NotFoundEntityException if the identifier does not exist
     */
    public Set<Speciality> findSpecialityByIds(@NonNull Set<Integer> ids) throws IllegalRequestArgumentException {
        return resolveSpecialities(ids);
    }

    /**
     * Creates a speciality that can be assigned to veterinarians.
     *
     * @param request the speciality name; any submitted ID is ignored
     * @return the saved speciality
     * @throws IllegalRequestArgumentException if the name already exists
     */
    public Speciality createSpeciality(@NonNull SpecialityDto request) throws IllegalRequestArgumentException {
        ensureUniqueSpecialityName(request.name(), null);
        return specialityRepository.save(new Speciality(request.name()));
    }

    /**
     * Renames an existing speciality while preserving its vet assignments.
     *
     * @param id speciality identifier
     * @param request the new name
     * @return the updated speciality
     * @throws NotFoundEntityException if the speciality with the id does not exist
     * @throws IllegalRequestArgumentException if another speciality has that name
     */
    public Speciality updateSpeciality(@NonNull Integer id, @NonNull SpecialityDto request) throws IllegalRequestArgumentException, NotFoundEntityException {
        Speciality speciality = findSpecialityById(id);
        ensureUniqueSpecialityName(request.name(), id);
        return specialityRepository.update(speciality.withName(request.name()));
    }

    /**
     * Creates a veterinarian from the HTML form.
     *
     * @param form submitted names and speciality IDs
     * @return the saved veterinarian with its generated ETag
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    public Vet create(@NonNull VetForm form) throws IllegalRequestArgumentException, NotFoundEntityException {
        Set<Speciality> selectedSpecialities;
        selectedSpecialities = resolveSpecialities(form.specialityIds());
        Vet saved = vetRepository.save(form.toVet(selectedSpecialities));
        return findById(saved.id());
    }

    /**
     * Updates a vet and its selected speciality links using the ETag loaded by the client.
     *
     * @param vet the current persisted vet
     * @param vetForm submitted names, speciality identifiers, and ETag
     * @return the updated vet with its newly generated ETag
     * @throws PreconditionRequiredException if the ETag is absent or blank
     * @throws PreconditionFailedException if the veterinarian has changed
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    @Transactional(rollbackOn = Exception.class)
    public Vet update(@NonNull Vet vet, @NonNull VetForm vetForm, String etag)
            throws PreconditionRequiredException, PreconditionFailedException, IllegalRequestArgumentException, NotFoundEntityException {
        return updateVersioned(vet, vetForm.firstName(), vetForm.lastName(), vetForm.specialityIds(), etag);
    }

    /**
     * Deletes a vet and its speciality links if the supplied ETag is still current.
     *
     * @param id veterinarian identifier
     * @param expectedEtag the loaded ETag, optionally enclosed in double quotes
     * @throws NotFoundEntityException if the identifier does not exist
     * @throws PreconditionRequiredException if the ETag is absent or blank
     * @throws PreconditionFailedException if the veterinarian has changed
     */
    @Transactional(rollbackOn = Exception.class)
    public void delete(Integer id, String expectedEtag)
            throws PreconditionRequiredException, PreconditionFailedException, NotFoundEntityException {
        expectedEtag = validateAndUnquoteEtag(expectedEtag);
        Vet vet = findById(id).withEtag(expectedEtag);
        deleteVet(vet);
    }

    private void deleteVet(@NonNull Vet vet) throws PreconditionFailedException {
        try {
            vetRepository.deleteSpecialityLinks(vet.id());
            vetRepository.delete(vet);
        } catch (OptimisticLockException e) {
            throw new PreconditionFailedException();
        }
    }

    private Vet updateVersioned(Vet updated) throws PreconditionFailedException, NotFoundEntityException {
        try {
            // Replace assignment rows explicitly so updating a vet cannot update speciality records.
            vetRepository.deleteSpecialityLinks(updated.id());
            vetRepository.update(updated);
            updated.specialities().forEach(speciality ->
                    vetRepository.insertSpecialityLink(updated.id(), speciality.id()));
            return findById(updated.id());
        } catch (OptimisticLockException stale) {
            throw new PreconditionFailedException();
        }
    }

    private Vet updateVersioned(Vet current,
                                String firstName,
                                String lastName,
                                Set<Integer> specialityIds,
                                String expectedEtag) throws PreconditionFailedException, IllegalRequestArgumentException, PreconditionRequiredException, NotFoundEntityException {
        expectedEtag = validateAndUnquoteEtag(expectedEtag);
        Set<Speciality> selectedSpecialities = resolveSpecialities(specialityIds);
        return updateVersioned(new Vet(current.id(), firstName, lastName, selectedSpecialities, expectedEtag));
    }

    private String validateAndUnquoteEtag(String etag) throws PreconditionRequiredException {
        if (etag == null || etag.isBlank()) {
            throw new PreconditionRequiredException();
        }
        if (etag.length() >= 2 && etag.startsWith("\"") && etag.endsWith("\"")) {
            return etag.substring(1, etag.length() - 1);
        }
        return etag;
    }

    private Set<Speciality> resolveSpecialities(@NonNull Set<Integer> requestedIds) throws IllegalRequestArgumentException {
        if (requestedIds.isEmpty()) {
            return Set.of();
        }
        if (requestedIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalRequestArgumentException("Each speciality must have an id.");
        }
        Set<Speciality> resolved = new HashSet<>(specialityRepository.findAllByIdIn(requestedIds));
        if (resolved.size() != requestedIds.size()) {
            throw new IllegalRequestArgumentException("One or more specialities were not found.");
        }
        return resolved;
    }

    private void ensureUniqueSpecialityName(String name, Integer currentId) throws IllegalRequestArgumentException {
        if (specialityRepository.findByName(name).filter(existing -> !existing.id().equals(currentId)).isPresent()) {
            throw new IllegalRequestArgumentException("Speciality name already exists.");
        }
    }

}
