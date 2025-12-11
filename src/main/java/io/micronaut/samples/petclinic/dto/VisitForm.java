package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.samples.petclinic.model.Visit;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Data Transfer Object for Visit form submissions.
 * Handles form binding and validation for creating visits.
 */
@Introspected
@Serdeable
public class VisitForm {

    @NotBlank(message = "Visit date is required")
    private String date;

    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;

    /**
     * Default constructor required for form binding.
     */
    public VisitForm() {
    }

    /**
     * Converts this form DTO to a Visit entity.
     *
     * @return a new Visit entity populated with form data
     */
    public Visit toVisit() {
        Visit visit = new Visit();
        visit.setDate(parseDate());
        visit.setDescription(this.description);
        return visit;
    }

    /**
     * Creates a VisitForm from an existing Visit entity.
     * Useful for populating edit forms.
     *
     * @param visit the visit entity
     * @return a new VisitForm populated with visit data
     */
    public static VisitForm fromVisit(Visit visit) {
        VisitForm form = new VisitForm();
        if (visit.getDate() != null) {
            form.setDate(visit.getDate().toString());
        }
        form.setDescription(visit.getDescription());
        return form;
    }

    /**
     * Parses the date string to LocalDate.
     *
     * @return the parsed LocalDate, or null if parsing fails
     */
    @Nullable
    public LocalDate parseDate() {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Getters and Setters

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "VisitForm{" +
                "date='" + date + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
