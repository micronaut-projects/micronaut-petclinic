package io.micronaut.samples.petclinic.repository;

import io.micronaut.data.repository.CrudRepository;
import io.micronaut.samples.petclinic.model.PetCareDocument;

/**
 * Repository for pet-care source documents.
 * <p>
 * The Oracle-specific repository bean extends this interface.
 */
public interface PetCareDocumentRepository extends CrudRepository<PetCareDocument, Integer> {
}
