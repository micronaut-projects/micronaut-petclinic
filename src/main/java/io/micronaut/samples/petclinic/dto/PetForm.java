package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.samples.petclinic.model.Pet;
import io.micronaut.samples.petclinic.model.PetType;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Data Transfer Object for Pet form submissions.
 * Handles form binding and validation for creating/updating pets.
 */
@Introspected
@Serdeable
public class PetForm {

    private Integer id;

    @NotBlank(message = "Pet name is required")
    @Size(min = 1, max = 30, message = "Pet name must be between 1 and 30 characters")
    private String name;

    @NotBlank(message = "Birth date is required")
    private String birthDate;

    @NotNull(message = "Pet type is required")
    private Integer typeId;

    /**
     * Default constructor required for form binding.
     */
    public PetForm() {
    }

    /**
     * Converts this form DTO to a Pet entity.
     * Note: PetType must be set separately after looking up by typeId.
     *
     * @return a new Pet entity populated with form data
     */
    public Pet toPet() {
        Pet pet = new Pet();
        pet.setName(this.name);
        pet.setBirthDate(parseBirthDate());
        return pet;
    }

    /**
     * Updates an existing Pet entity with form data.
     * Note: PetType must be set separately after looking up by typeId.
     *
     * @param pet the pet to update
     * @return the updated pet
     */
    public Pet updatePet(Pet pet) {
        pet.setName(this.name);
        pet.setBirthDate(parseBirthDate());
        return pet;
    }

    /**
     * Creates a PetForm from an existing Pet entity.
     * Useful for populating edit forms.
     *
     * @param pet the pet entity
     * @return a new PetForm populated with pet data
     */
    public static PetForm fromPet(Pet pet) {
        PetForm form = new PetForm();
        form.setId(pet.getId());
        form.setName(pet.getName());
        if (pet.getBirthDate() != null) {
            form.setBirthDate(pet.getBirthDate().toString());
        }
        if (pet.getType() != null) {
            form.setTypeId(pet.getType().getId());
        }
        return form;
    }

    /**
     * Parses the birth date string to LocalDate.
     *
     * @return the parsed LocalDate, or null if parsing fails
     */
    @Nullable
    public LocalDate parseBirthDate() {
        if (birthDate == null || birthDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(birthDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    @Override
    public String toString() {
        return "PetForm{" +
                "name='" + name + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", typeId=" + typeId +
                '}';
    }
}
