package io.micronaut.samples.petclinic.service;

import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.samples.petclinic.exception.IllegalRequestArgumentException;
import io.micronaut.samples.petclinic.exception.NotFoundEntityException;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.samples.petclinic.repository.PetRepository;
import io.micronaut.samples.petclinic.repository.PetTypeRepository;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** Business operations for pets, pet types, and pet photo references. */
@Singleton
@NullMarked
public class PetService {

    private static final Logger LOG = LoggerFactory.getLogger(PetService.class);

    private final PetRepository petRepository;
    private final PetTypeRepository petTypeRepository;
    private final FileStorage fileStorage;
    private final PetPhotoValidator petPhotoValidator;

    /**
     * Creates the service with its repositories, file storage, and photo validator.
     *
     * @param petRepository repository for pets
     * @param fileStorage the file storage backend
     * @param petTypeRepository repository for pet types
     * @param petPhotoValidator validator for uploaded photo content
     */
    public PetService(PetRepository petRepository, FileStorage fileStorage, PetTypeRepository petTypeRepository, PetPhotoValidator petPhotoValidator) {
        this.petRepository = petRepository;
        this.fileStorage = fileStorage;
        this.petTypeRepository = petTypeRepository;
        this.petPhotoValidator = petPhotoValidator;
    }

    /**
     * Locks and reloads the pet, then saves editable details while preserving its current photo key.
     *
     * @param pet the submitted pet details, including the required owner
     * @throws NotFoundEntityException if the pet does not exist or belongs to a different owner
     */
    @Transactional
    public void updatePetDetails(Pet pet) throws NotFoundEntityException {
        Pet current = findByIdAndOwnerIdForUpdate(pet.id(), pet.getOwnerId());
        savePet(current.withName(pet.name()).withBirthDate(pet.birthDate()).withType(pet.type()));
    }

    /**
     * Locks and reloads a pet belonging to the required owner.
     * Call within a transaction so the lock remains held through the subsequent save.
     *
     * @param id the pet identifier
     * @param ownerId the required owner identifier
     * @return the locked pet with its associations loaded
     * @throws NotFoundEntityException if the pet is missing or belongs to a different owner
     */
    public Pet findByIdAndOwnerIdForUpdate(Integer id, Integer ownerId) throws NotFoundEntityException {
        Pet pet = findByIdForUpdate(id);
        if (!ownerId.equals(pet.getOwnerId())) {
            throw new NotFoundEntityException();
        }
        return pet;
    }

    /**
     * Locks and reloads a pet within the caller's transaction.
     *
     * @param id the pet identifier
     * @return the locked pet with its associations loaded
     * @throws NotFoundEntityException if the pet does not exist
     */
    public Pet findByIdForUpdate(Integer id) throws NotFoundEntityException {
        return petRepository.findByIdForUpdate(id).orElseThrow(NotFoundEntityException::new);
    }

    /**
     * Validates an upload, locks and reloads its pet, and saves the new photo key.
     * Closes the upload and attempts to remove the old file after saving, or the new file if saving fails.
     *
     * @param pet the pet whose photo is being replaced, including its required owner
     * @param upload the completed multipart image; this method closes it
     * @throws NotFoundEntityException if the pet is missing or belongs to a different owner
     * @throws IllegalRequestArgumentException if file validation fails
     * @throws IOException if reading or storing the upload fails
     */
    @Transactional
    public void replacePetPhoto(Pet pet, CompletedFileUpload upload)
            throws NotFoundEntityException, IllegalRequestArgumentException, IOException {
        try (upload) {
            var photo = petPhotoValidator.validate(upload);
            Pet current = findByIdAndOwnerIdForUpdate(pet.id(), pet.getOwnerId());
            String filename = fileStorage.store(photo.bytes(), photo.mediaType());
            boolean saved = false;
            try {
                savePet(current.withPhotoFilename(filename));
                saved = true;
            } finally {
                if (!saved) {
                    cleanupPhoto(filename);
                }
            }
            cleanupPhoto(current.photoFilename());
        }
    }

    /**
     * Find a pet by ID.
     * @param id the pet ID
     * @return the persisted pet
     * @throws NotFoundEntityException if the pet does not exist
     */
    public Pet findById(Integer id) throws NotFoundEntityException {
        return petRepository.findById(id).orElseThrow(NotFoundEntityException::new);
    }

    /**
     * Finds a pet belonging to the specified owner, with owner, type, and visits loaded.
     *
     * @param petId the pet identifier
     * @param ownerId the required owner identifier
     * @return the persisted pet
     * @throws NotFoundEntityException if the pet does not exist or belongs to a different owner
     */
    public Pet findByIdAndOwnerId(Integer petId, Integer ownerId) throws NotFoundEntityException {
        return petRepository.findByIdAndOwnerId(petId, ownerId).orElseThrow(NotFoundEntityException::new);
    }

    /**
     * Save a pet (create or update).
     * @param pet the pet to save
     * @return the persisted pet returned by the repository
     */
    @Transactional
    public Pet savePet(Pet pet) {
        if (pet.isNew()) {
            return petRepository.save(pet);
        } else {
            return petRepository.update(pet);
        }
    }

    /**
     * Delete a pet by ID.
     * @param id the pet ID
     */
    @Transactional
    public void deletePet(Integer id) {
        petRepository.deleteById(id);
    }

    /**
     * Find all pet types.
     * @return list of all pet types
     */
    public List<PetType> findPetTypes() {
        return petTypeRepository.findAllOrderByName();
    }

    /**
     * Find a pet type by ID.
     * @param id the pet type ID
     * @return the pet type, if found
     */
    public Optional<PetType> findPetTypeById(Integer id) {
        return petTypeRepository.findById(id);
    }

    /**
     * Attempts file cleanup without hiding a save failure or failing a completed upload.
     * Logs cleanup errors for later file maintenance.
     *
     * @param filename the obsolete storage key, or {@code null} when there is no file to remove
     */
    private void cleanupPhoto(@Nullable String filename) {
        if (filename == null) {
            return;
        }
        try {
            fileStorage.delete(filename);
        } catch (IOException | IllegalRequestArgumentException failure) {
            LOG.warn("Could not clean up pet photo {}", filename, failure);
        }
    }

    /**
     * Loads the file associated with a pet's photo key.
     *
     * @param pet the pet whose photo is requested
     * @return the stored file bytes and inferred media type
     * @throws NotFoundEntityException if the pet has no photo or the stored file is missing
     * @throws IllegalRequestArgumentException if the stored key or file is invalid
     * @throws IOException if reading the stored file fails
     */
    public FileStorage.StoredFile loadPetPhoto(Pet pet) throws IOException, IllegalRequestArgumentException, NotFoundEntityException {
        if (pet.photoFilename() == null) {
            throw new NotFoundEntityException("Pet has no photo.");
        }
        return fileStorage.load(pet.photoFilename());
    }
}
