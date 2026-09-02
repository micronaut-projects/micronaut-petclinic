@import io.micronaut.samples.petclinic.dto.PetForm
@import io.micronaut.samples.petclinic.model.Owner
@import io.micronaut.samples.petclinic.model.PetType
@import java.util.Collection

@param PetForm pet
@param Owner owner
@param Collection<PetType> types
@param boolean isNew
@param java.util.Map<String, String> validationErrors
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1>
                <i class="${isNew ? "fas fa-plus-circle me-3" : "fas fa-edit me-3"}"></i>
                ${isNew ? msg.text("pet.new") : msg.text("pet.editPet")}
            </h1>
            @if(owner != null)
                <p>
                    <i class="fas fa-user me-2"></i>${msg.text("pet.owner")}: ${owner.getFirstName()} ${owner.getLastName()}
                </p>
            @endif
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="row justify-content-center">
            <div class="col-lg-6">
                <a href="/owners/${owner.id()}" class="btn btn-secondary mb-4">
                    <i class="fas fa-arrow-left me-2"></i>${msg.text("pet.backToOwner")}
                </a>

                <div class="form-container animate-fade-in-up">
                    <div class="text-center mb-4">
                        <div class="feature-icon gradient-2 mx-auto mb-3">
                            <i class="fas fa-paw"></i>
                        </div>
                        <h4>${isNew ? msg.text("pet.registerNew") : msg.text("pet.updateInfo")}</h4>
                    </div>

                    @if(validationErrors != null && !validationErrors.isEmpty())
                        <div class="alert alert-danger" role="alert">
                            ${msg.text("validation.fixErrors")}
                        </div>
                    @endif
 
                    <form action="${isNew ? ("/owners/" + owner.id() + "/pets/new") : ("/owners/" + owner.id() + "/pets/" + pet.id() + "/edit")}" method="post" class="needs-validation" novalidate>
                        <div class="mb-4">
                            <label for="name" class="form-label">
                                <i class="fas fa-tag me-2 text-primary"></i>${msg.text("pet.name")}
                            </label>
                            <input type="text" class="form-control ${validationErrors != null && validationErrors.containsKey("name") ? "is-invalid" : ""}" id="name" name="name"
                                   value="${pet != null && pet.name() != null ? pet.name() : ""}" required>
                            @if(validationErrors != null && validationErrors.containsKey("name"))
                                <div class="invalid-feedback">${validationErrors.get("name")}</div>
                            @else
                                <div class="invalid-feedback">${msg.text("validation.petName.required")}</div>
                            @endif
                        </div>
 
                        <div class="mb-4">
                            <label for="birthDate" class="form-label">
                                <i class="fas fa-birthday-cake me-2 text-primary"></i>${msg.text("pet.birthDate")}
                            </label>
                            <input type="date" class="form-control ${validationErrors != null && validationErrors.containsKey("birthDate") ? "is-invalid" : ""}" id="birthDate" name="birthDate"
                                   value="${pet != null && pet.birthDate() != null ? pet.birthDate().toString() : ""}" required>
                            @if(validationErrors != null && validationErrors.containsKey("birthDate"))
                                <div class="invalid-feedback">${validationErrors.get("birthDate")}</div>
                            @else
                                <div class="invalid-feedback">${msg.text("validation.birthDate.required")}</div>
                            @endif
                        </div>
 
                        <div class="mb-4">
                            <label for="typeId" class="form-label">
                                <i class="fas fa-paw me-2 text-primary"></i>${msg.text("pet.type")}
                            </label>
                            <select class="form-select ${validationErrors != null && validationErrors.containsKey("typeId") ? "is-invalid" : ""}" id="typeId" name="typeId" required>
                                <option value="">${msg.text("pet.selectType")}</option>
                                @for(PetType type : types)
                                    @if(pet != null && pet.typeId() != null && pet.typeId().equals(type.id()))
                                        <option value="${type.id()}" selected>${type.name()}</option>
                                    @else
                                        <option value="${type.id()}">${type.name()}</option>
                                    @endif
                                @endfor
                            </select>
                            @if(validationErrors != null && validationErrors.containsKey("typeId"))
                                <div class="invalid-feedback">${validationErrors.get("typeId")}</div>
                            @else
                                <div class="invalid-feedback">${msg.text("validation.petType.required")}</div>
                            @endif
                        </div>

                        <hr class="my-4">

                        <div class="d-flex gap-3">
                            <button type="submit" class="btn btn-primary btn-lg flex-grow-1">
                                <i class="${isNew ? "fas fa-plus me-2" : "fas fa-save me-2"}"></i>
                                ${isNew ? msg.text("button.addPet") : msg.text("button.update")}
                            </button>
                            <a href="/owners/${owner.id()}" class="btn btn-secondary btn-lg">
                                <i class="fas fa-times me-2"></i>${msg.text("button.cancel")}
                            </a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
