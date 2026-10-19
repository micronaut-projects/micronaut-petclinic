package io.micronaut.samples.petclinic.service;

import io.micronaut.data.exceptions.OptimisticLockException;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.samples.petclinic.dto.SpecialityDto;
import io.micronaut.samples.petclinic.dto.VetDto;
import io.micronaut.samples.petclinic.dto.VetForm;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
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
import java.util.Set;
import java.util.stream.Collectors;

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
     * @throws NotFoundException if the identifier does not exist
     */
    public Vet findById(Integer id) {
        Vet vet = vetRepository.findById(id).orElseThrow(NotFoundException::new);
        try {
            validateEtag(vet.etag());
        } catch (MissingEtagException e) {
            throw new IllegalStateException("Fetched vet etag can't be null.");
        }
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
     * @throws NotFoundException if the identifier does not exist
     */
    public Speciality findSpecialityById(Integer id) {
        return specialityRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    /**
     * Creates a speciality that can be assigned to veterinarians.
     *
     * @param request the speciality name; any submitted ID is ignored
     * @return the saved speciality
     * @throws IllegalRequestArgumentException if the name already exists
     */
    public Speciality createSpeciality(@NonNull SpecialityDto request) throws IllegalRequestArgumentException {
        try {
            ensureUniqueSpecialityName(request.name(), null);
        } catch (DomainValidationException e) {
            throw new IllegalRequestArgumentException(e.getMessage());
        }
        return specialityRepository.save(new Speciality(request.name()));
    }

    /**
     * Renames an existing speciality while preserving its vet assignments.
     *
     * @param id speciality identifier
     * @param request the new name
     * @return the updated speciality
     * @throws NotFoundException if the identifier does not exist
     * @throws IllegalRequestArgumentException if another speciality has that name
     */
    public Speciality updateSpeciality(@NonNull Integer id, @NonNull SpecialityDto request) throws IllegalRequestArgumentException {
        Speciality speciality = specialityRepository.findById(id).orElseThrow(NotFoundException::new);
        try {
            ensureUniqueSpecialityName(request.name(), id);
        } catch (DomainValidationException e) {
            throw new IllegalRequestArgumentException(e.getMessage());
        }
        return specialityRepository.update(speciality.withName(request.name()));
    }

    /**
     * Creates a veterinarian from the HTML form.
     *
     * @param form submitted names and speciality IDs
     * @return the saved veterinarian with its generated ETag
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    public Vet create(@NonNull VetForm form) throws IllegalRequestArgumentException {
        Set<Speciality> selectedSpecialities;
        try {
            selectedSpecialities = resolveSpecialities(form.specialityIds());
        } catch (DomainValidationException e) {
            throw new IllegalRequestArgumentException(e.getMessage());
        }
        Vet saved = vetRepository.save(form.toVet(null, selectedSpecialities));
        return findById(saved.id());
    }

    /**
     * Updates a vet and its selected speciality links using the ETag loaded by the client.
     *
     * @param vet the current persisted vet
     * @param vetDto submitted names, speciality identifiers, and ETag
     * @return the updated vet with its newly generated ETag
     * @throws PreconditionRequiredException if the ETag is absent or blank
     * @throws PreconditionFailedException if the veterinarian has changed
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    @Transactional(rollbackOn = Exception.class)
    public Vet update(@NonNull Vet vet, @NonNull VetDto vetDto)
            throws PreconditionRequiredException, PreconditionFailedException, IllegalRequestArgumentException {
        Set<Integer> specialityIds = vetDto.specialities().stream()
                .map(SpecialityDto::id).collect(Collectors.toSet());
        try {
            return updateVersioned(vet, vetDto.firstName(), vetDto.lastName(), specialityIds, vetDto.etag());
        } catch (MissingEtagException e) {
            throw new PreconditionRequiredException(e.getMessage(), e);
        } catch (StaleEtagException e) {
            throw new PreconditionFailedException(e.getMessage(), e);
        } catch (DomainValidationException e) {
            throw new IllegalRequestArgumentException(e.getMessage());
        }
    }

    /**
     * Deletes a vet and its speciality links if the supplied ETag is still current.
     *
     * @param id veterinarian identifier
     * @param expectedEtag the loaded ETag, optionally enclosed in double quotes
     * @throws NotFoundException if the identifier does not exist
     * @throws PreconditionRequiredException if the ETag is absent or blank
     * @throws PreconditionFailedException if the veterinarian has changed
     */
    @Transactional(rollbackOn = Exception.class)
    public void delete(Integer id, String expectedEtag)
            throws PreconditionRequiredException, PreconditionFailedException {
        try {
            validateEtag(expectedEtag);
            Vet vet = findById(id).withEtag(expectedEtag);
            deleteVet(vet);
        } catch (MissingEtagException e) {
            throw new PreconditionRequiredException(e.getMessage(), e);
        } catch (StaleEtagException e) {
            throw new PreconditionFailedException(e.getMessage(), e);
        }
    }

    private void validateEtag(String etag) throws MissingEtagException {
        if (etag == null || etag.isBlank()) {
            throw new MissingEtagException();
        }
    }

    private void deleteVet(@NonNull Vet vet) throws StaleEtagException {
        try {
            vetRepository.deleteSpecialityLinks(vet.id());
            vetRepository.delete(vet);
        } catch (OptimisticLockException e) {
            throw new StaleEtagException();
        }

    }

    private Vet updateVersioned(Vet updated) throws StaleEtagException {
        try {
            // The JDBC update cascade adds links but does not remove deselected ones.
            // The caller's transaction rolls this back if the ETag check fails.
            vetRepository.deleteSpecialityLinks(updated.id());
            vetRepository.update(updated);
            return findById(updated.id());
        } catch (OptimisticLockException stale) {
            throw new StaleEtagException();
        }
    }

    private Vet updateVersioned(Vet current,
                                String firstName,
                                String lastName,
                                Set<Integer> specialityIds,
                                String expectedEtag) throws StaleEtagException, MissingEtagException, DomainValidationException {
        validateEtag(expectedEtag);
        Set<Speciality> selectedSpecialities = resolveSpecialities(specialityIds);
        return updateVersioned(new Vet(current.id(), firstName, lastName, selectedSpecialities, expectedEtag));
    }

    private Set<Speciality> resolveSpecialities(@NonNull Set<Integer> requestedIds) throws DomainValidationException {
        if (requestedIds.isEmpty()) {
            return Set.of();
        }
        if (requestedIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new DomainValidationException("Each speciality must have an id.");
        }
        Set<Speciality> resolved = new HashSet<>(specialityRepository.findAllByIdIn(requestedIds));
        if (resolved.size() != requestedIds.size()) {
            throw new DomainValidationException("One or more specialities were not found.");
        }
        return resolved;
    }

    private void ensureUniqueSpecialityName(String name, Integer currentId) throws DomainValidationException {
        if (specialityRepository.findByName(name).filter(existing -> !existing.id().equals(currentId)).isPresent()) {
            throw new DomainValidationException("Speciality name already exists.");
        }
    }

}
