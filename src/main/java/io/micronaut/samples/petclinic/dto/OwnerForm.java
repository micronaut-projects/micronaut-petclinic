package io.micronaut.samples.petclinic.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.samples.petclinic.model.Owner;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Owner form submissions.
 * Handles form binding and validation for creating/updating owners.
 */
@Introspected
@Serdeable
public class OwnerForm {

    private Integer id;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 30, message = "First name must be between 1 and 30 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 30, message = "Last name must be between 1 and 30 characters")
    private String lastName;

    @NotBlank(message = "Address is required")
    @Size(min = 1, max = 255, message = "Address must be between 1 and 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 1, max = 80, message = "City must be between 1 and 80 characters")
    private String city;

    @NotBlank(message = "Telephone is required")
    @Pattern(regexp = "\\d{10}", message = "Telephone must be a 10-digit number")
    private String telephone;

    /**
     * Default constructor required for form binding.
     */
    public OwnerForm() {
    }

    /**
     * Converts this form DTO to an Owner entity.
     *
     * @return a new Owner entity populated with form data
     */
    public Owner toOwner() {
        Owner owner = new Owner();
        owner.setFirstName(this.firstName);
        owner.setLastName(this.lastName);
        owner.setAddress(this.address);
        owner.setCity(this.city);
        owner.setTelephone(this.telephone);
        return owner;
    }

    /**
     * Updates an existing Owner entity with form data.
     *
     * @param owner the owner to update
     * @return the updated owner
     */
    public Owner updateOwner(Owner owner) {
        owner.setFirstName(this.firstName);
        owner.setLastName(this.lastName);
        owner.setAddress(this.address);
        owner.setCity(this.city);
        owner.setTelephone(this.telephone);
        return owner;
    }

    /**
     * Creates an OwnerForm from an existing Owner entity.
     * Useful for populating edit forms.
     *
     * @param owner the owner entity
     * @return a new OwnerForm populated with owner data
     */
    public static OwnerForm fromOwner(Owner owner) {
        OwnerForm form = new OwnerForm();
        form.setId(owner.getId());
        form.setFirstName(owner.getFirstName());
        form.setLastName(owner.getLastName());
        form.setAddress(owner.getAddress());
        form.setCity(owner.getCity());
        form.setTelephone(owner.getTelephone());
        return form;
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    @Override
    public String toString() {
        return "OwnerForm{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", telephone='" + telephone + '\'' +
                '}';
    }
}
