@import io.micronaut.samples.petclinic.dto.PetForm
@import io.micronaut.samples.petclinic.model.Owner
@import io.micronaut.samples.petclinic.model.PetType
@import java.util.Collection

@param PetForm pet
@param Owner owner
@param Collection<PetType> types
@param boolean isNew
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request

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
                <a href="/owners/${owner.getId()}" class="btn btn-secondary mb-4">
                    <i class="fas fa-arrow-left me-2"></i>${msg.text("pet.backToOwner")}
                </a>

                <div class="form-container animate-fade-in-up">
                    <div class="text-center mb-4">
                        <div class="feature-icon gradient-2 mx-auto mb-3">
                            <i class="fas fa-paw"></i>
                        </div>
                        <h4>${isNew ? msg.text("pet.registerNew") : msg.text("pet.updateInfo")}</h4>
                    </div>

                    <form action="${isNew ? ("/owners/" + owner.getId() + "/pets/new") : ("/owners/" + owner.getId() + "/pets/" + pet.getId() + "/edit")}" method="post" class="needs-validation" novalidate>
                        <div class="mb-4">
                            <label for="name" class="form-label">
                                <i class="fas fa-tag me-2 text-primary"></i>${msg.text("pet.name")}
                            </label>
                            <input type="text" class="form-control" id="name" name="name"
                                   value="${pet != null && pet.getName() != null ? pet.getName() : ""}" required>
                            <div class="invalid-feedback">${msg.text("validation.petName.required")}</div>
                        </div>

                        <div class="mb-4">
                            <label for="birthDate" class="form-label">
                                <i class="fas fa-birthday-cake me-2 text-primary"></i>${msg.text("pet.birthDate")}
                            </label>
                            <input type="date" class="form-control" id="birthDate" name="birthDate"
                                   value="${pet != null && pet.getBirthDate() != null ? pet.getBirthDate().toString() : ""}" required>
                            <div class="invalid-feedback">${msg.text("validation.birthDate.required")}</div>
                        </div>

                        <div class="mb-4">
                            <label for="typeId" class="form-label">
                                <i class="fas fa-paw me-2 text-primary"></i>${msg.text("pet.type")}
                            </label>
                            <select class="form-select" id="typeId" name="typeId" required>
                                <option value="">${msg.text("pet.selectType")}</option>
                                @for(PetType type : types)
                                    @if(pet != null && pet.getTypeId() != null && pet.getTypeId().equals(type.getId()))
                                        <option value="${type.getId()}" selected>${type.getName()}</option>
                                    @else
                                        <option value="${type.getId()}">${type.getName()}</option>
                                    @endif
                                @endfor
                            </select>
                            <div class="invalid-feedback">${msg.text("validation.petType.required")}</div>
                        </div>

                        <hr class="my-4">

                        <div class="d-flex gap-3">
                            <button type="submit" class="btn btn-primary btn-lg flex-grow-1">
                                <i class="${isNew ? "fas fa-plus me-2" : "fas fa-save me-2"}"></i>
                                ${isNew ? msg.text("button.addPet") : msg.text("button.update")}
                            </button>
                            <a href="/owners/${owner.getId()}" class="btn btn-secondary btn-lg">
                                <i class="fas fa-times me-2"></i>${msg.text("button.cancel")}
                            </a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request)
