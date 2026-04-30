@import io.micronaut.samples.petclinic.dto.OwnerForm

@param OwnerForm owner
@param boolean isNew
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1>
                <i class="${isNew ? "fas fa-user-plus me-3" : "fas fa-user-edit me-3"}"></i>
                ${isNew ? msg.text("owner.new") : msg.text("owner.editOwner")}
            </h1>
            <p>${isNew ? msg.text("form.newOwner.desc") : msg.text("form.editOwner.desc")}</p>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <a href="${isNew ? "/owners/find" : ("/owners/" + owner.getId())}" class="btn btn-secondary mb-4">
                    <i class="fas fa-arrow-left me-2"></i>${msg.text("button.back")}
                </a>

                <div class="form-container animate-fade-in-up">
                    <form action="${isNew ? "/owners/new" : ("/owners/" + owner.getId() + "/edit")}" method="post" class="needs-validation" novalidate>
                        <div class="row mb-4">
                            <div class="col-md-6 mb-3 mb-md-0">
                                <label for="firstName" class="form-label">
                                    <i class="fas fa-user me-2 text-primary"></i>${msg.text("owner.firstName")}
                                </label>
                                <input type="text" class="form-control" id="firstName" name="firstName"
                                       value="${owner != null && owner.getFirstName() != null ? owner.getFirstName() : ""}" required>
                                <div class="invalid-feedback">${msg.text("validation.firstName.required")}</div>
                            </div>
                            <div class="col-md-6">
                                <label for="lastName" class="form-label">
                                    <i class="fas fa-user me-2 text-primary"></i>${msg.text("owner.lastName")}
                                </label>
                                <input type="text" class="form-control" id="lastName" name="lastName"
                                       value="${owner != null && owner.getLastName() != null ? owner.getLastName() : ""}" required>
                                <div class="invalid-feedback">${msg.text("validation.lastName.required")}</div>
                            </div>
                        </div>

                        <div class="mb-4">
                            <label for="address" class="form-label">
                                <i class="fas fa-home me-2 text-primary"></i>${msg.text("owner.address")}
                            </label>
                            <input type="text" class="form-control" id="address" name="address"
                                   value="${owner != null && owner.getAddress() != null ? owner.getAddress() : ""}" required>
                            <div class="invalid-feedback">${msg.text("validation.address.required")}</div>
                        </div>

                        <div class="row mb-4">
                            <div class="col-md-6 mb-3 mb-md-0">
                                <label for="city" class="form-label">
                                    <i class="fas fa-city me-2 text-primary"></i>${msg.text("owner.city")}
                                </label>
                                <input type="text" class="form-control" id="city" name="city"
                                       value="${owner != null && owner.getCity() != null ? owner.getCity() : ""}" required>
                                <div class="invalid-feedback">${msg.text("validation.city.required")}</div>
                            </div>
                            <div class="col-md-6">
                                <label for="telephone" class="form-label">
                                    <i class="fas fa-phone me-2 text-primary"></i>${msg.text("owner.telephone")}
                                </label>
                                <input type="tel" class="form-control" id="telephone" name="telephone"
                                       value="${owner != null && owner.getTelephone() != null ? owner.getTelephone() : ""}" pattern="[0-9]{10}" required>
                                <div class="invalid-feedback">${msg.text("validation.telephone")}</div>
                            </div>
                        </div>

                        <hr class="my-4">

                        <div class="d-flex gap-3">
                            <button type="submit" class="btn btn-primary btn-lg flex-grow-1">
                                <i class="${isNew ? "fas fa-plus me-2" : "fas fa-save me-2"}"></i>
                                ${isNew ? msg.text("button.addOwner") : msg.text("button.updateOwner")}
                            </button>
                            <a href="${isNew ? "/owners/find" : ("/owners/" + owner.getId())}" class="btn btn-secondary btn-lg">
                                <i class="fas fa-times me-2"></i>${msg.text("button.cancel")}
                            </a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request)
