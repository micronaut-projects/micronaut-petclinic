package io.micronaut.samples.petclinic.service;

import io.micronaut.data.exceptions.OptimisticLockException;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.server.exceptions.NotFoundException;
import io.micronaut.samples.petclinic.dto.SpecialityDto;
import io.micronaut.samples.petclinic.dto.VetDto;
import io.micronaut.samples.petclinic.dto.VetForm;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.MissingEtagException;
import io.micronaut.samples.petclinic.exception.StaleEtagException;
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
        return vetRepository.findById(id).orElseThrow(NotFoundException::new);
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
        ensureUniqueSpecialityName(request.name(), null);
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
        Speciality current = specialityRepository.findById(id)
                .orElseThrow(NotFoundException::new);
        ensureUniqueSpecialityName(request.name(), id);
        return specialityRepository.update(new Speciality(id, request.name(), current.vets()));
    }

    /**
     * Creates a veterinarian from the HTML form.
     *
     * @param form submitted names and speciality IDs
     * @return the saved veterinarian with its generated ETag
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    @Transactional
    public Vet create(@NonNull VetForm form) throws IllegalRequestArgumentException {
        Set<Speciality> selectedSpecialities = resolveSpecialities(form.specialityIds());
        Vet saved = vetRepository.save(new Vet(null, form.firstName(), form.lastName(), selectedSpecialities, null));
        return findById(saved.id());
    }

    /**
     * Creates a veterinarian using persisted speciality records.
     *
     * @param vet submitted names and speciality references; ID and ETag are ignored
     * @return the saved veterinarian with its generated ETag
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    @Transactional
    public Vet create(@NonNull Vet vet) throws IllegalRequestArgumentException {
        Set<Integer> specialityIds = vet.specialities().stream()
                .map(Speciality::id)
                .collect(Collectors.toSet());
        Set<Speciality> selectedSpecialities = resolveSpecialities(specialityIds);
        Vet newVet = new Vet(null, vet.firstName(), vet.lastName(), selectedSpecialities, null);
        return findById(vetRepository.save(newVet).id());
    }

    /**
     * Updates a vet and its selected speciality links using the ETag loaded by the client.
     *
     * @param vet the current persisted vet
     * @param vetDto submitted names, speciality identifiers, and ETag
     * @return the updated vet with its newly generated ETag
     * @throws MissingEtagException if the ETag is absent or blank
     * @throws StaleEtagException if the veterinarian has changed
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    @Transactional(rollbackOn = Exception.class)
    public Vet update(@NonNull Vet vet, @NonNull VetDto vetDto) throws MissingEtagException, StaleEtagException, IllegalRequestArgumentException {
        if (vetDto.etag() == null || vetDto.etag().isBlank()) {
            throw new MissingEtagException("Provide the ETag from the loaded entity when updating.");
        }

        Set<Integer> specialityIds = vetDto.specialities().stream()
                .map(SpecialityDto::id)
                .collect(Collectors.toSet());
        return updateVersioned(vet, vetDto.firstName(), vetDto.lastName(), specialityIds, vetDto.etag());
    }

    /**
     * Updates a veterinarian using the ETag captured when the edit form was loaded.
     *
     * @param current the persisted veterinarian
     * @param form submitted fields and the loaded ETag
     * @return the saved veterinarian with its current ETag
     * @throws HttpStatusException if the ID mismatches or the ETag is missing
     * @throws StaleEtagException if the veterinarian has changed
     * @throws IllegalRequestArgumentException if a speciality reference is invalid
     */
    @Transactional(rollbackOn = Exception.class)
    public Vet update(@NonNull Vet current, @NonNull VetForm form) throws StaleEtagException, IllegalRequestArgumentException {
        if (form.id() != null && !form.id().equals(current.id())) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Vet id in the form must match the URL.");
        }
        if (form.etag() == null || form.etag().isBlank()) {
            throw new HttpStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "Reload the veterinarian form before saving.");
        }
        return updateVersioned(current, form.firstName(), form.lastName(), form.specialityIds(), form.etag());
    }

    /**
     * Deletes a vet and its speciality links if the supplied ETag is still current.
     *
     * @param id veterinarian identifier
     * @param expectedEtag the loaded ETag, optionally enclosed in double quotes
     * @throws NotFoundException if the identifier does not exist
     * @throws MissingEtagException if the ETag is absent or blank
     * @throws StaleEtagException if the veterinarian has changed
     */
    public void delete(Integer id, String expectedEtag) throws StaleEtagException, MissingEtagException {
        if (expectedEtag == null || expectedEtag.isBlank()) {
            throw new MissingEtagException("Reload the veterinarian before deleting.");
        }
        try {
            vetRepository.delete(findById(id).withEtag(unquote(expectedEtag)));
        } catch (OptimisticLockException stale) {
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
                                String expectedEtag) throws StaleEtagException, IllegalRequestArgumentException {
        Set<Speciality> selectedSpecialities = resolveSpecialities(specialityIds);
        return updateVersioned(new Vet(current.id(), firstName, lastName, selectedSpecialities, unquote(expectedEtag)));
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

    private static String unquote(String etag) {
        String value = etag.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

}
