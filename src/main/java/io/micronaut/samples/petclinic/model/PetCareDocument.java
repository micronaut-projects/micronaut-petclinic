package io.micronaut.samples.petclinic.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

/**
 * A source document from the pet-care knowledge base.
 *
 * @param id the database identifier
 * @param title the human-readable document title
 * @param source the source label or URL
 * @param description a short description of the document
 */
@MappedEntity("PET_CARE_DOCUMENTS")
@Serdeable
public record PetCareDocument(
        @Id
        @GeneratedValue
        Integer id,

        @MappedProperty("TITLE")
        String title,

        @MappedProperty("SOURCE")
        String source,

        @MappedProperty("DESCRIPTION")
        String description
) {

    /**
     * Creates a document that has not yet been persisted.
     *
     * @param title the document title
     * @param source the source label
     * @param description the document description
     */
    public PetCareDocument(String title, String source, String description) {
        this(null, title, source, description);
    }
}
