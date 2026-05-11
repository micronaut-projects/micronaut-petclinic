@import io.micronaut.samples.petclinic.dto.OwnerForm

@param OwnerForm owner
@param boolean isNew
@param java.util.Map<String, String> validationErrors
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale

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
                <a href="${isNew ? "/owners/find" : ("/owners/" + owner.id())}" class="btn btn-secondary mb-4">
                    <i class="fas fa-arrow-left me-2"></i>${msg.text("button.back")}
                </a>

                <div class="form-container animate-fade-in-up">
                    @if(validationErrors != null && !validationErrors.isEmpty())
                        <div class="alert alert-danger" role="alert">
                            ${msg.text("validation.fixErrors")}
                        </div>
                    @endif
                    <form action="${isNew ? "/owners/new" : ("/owners/" + owner.id() + "/edit")}" method="post" class="needs-validation" novalidate>
                        <div class="row mb-4">
                            <div class="col-md-6 mb-3 mb-md-0">
                                <label for="firstName" class="form-label">
                                    <i class="fas fa-user me-2 text-primary"></i>${msg.text("owner.firstName")}
                                </label>
                                <input type="text" class="form-control ${validationErrors != null && validationErrors.containsKey("firstName") ? "is-invalid" : ""}" id="firstName" name="firstName"
                                       value="${owner != null && owner.firstName() != null ? owner.firstName() : ""}" required>
                                <div class="invalid-feedback">${validationErrors != null && validationErrors.containsKey("firstName") ? validationErrors.get("firstName") : msg.text("validation.firstName.required")}</div>
                            </div>
                            <div class="col-md-6">
                                <label for="lastName" class="form-label">
                                    <i class="fas fa-user me-2 text-primary"></i>${msg.text("owner.lastName")}
                                </label>
                                <input type="text" class="form-control ${validationErrors != null && validationErrors.containsKey("lastName") ? "is-invalid" : ""}" id="lastName" name="lastName"
                                       value="${owner != null && owner.lastName() != null ? owner.lastName() : ""}" required>
                                <div class="invalid-feedback">${validationErrors != null && validationErrors.containsKey("lastName") ? validationErrors.get("lastName") : msg.text("validation.lastName.required")}</div>
                            </div>
                        </div>

                        <div class="mb-4">
                            <label for="address" class="form-label">
                                <i class="fas fa-home me-2 text-primary"></i>${msg.text("owner.address")}
                            </label>
                            <input type="text" class="form-control ${validationErrors != null && validationErrors.containsKey("address") ? "is-invalid" : ""}" id="address" name="address"
                                   value="${owner != null && owner.address() != null ? owner.address() : ""}" required>
                            <div class="invalid-feedback">${validationErrors != null && validationErrors.containsKey("address") ? validationErrors.get("address") : msg.text("validation.address.required")}</div>
                        </div>

                        <div class="row mb-4">
                            <div class="col-md-6 mb-3 mb-md-0">
                                <label for="city" class="form-label">
                                    <i class="fas fa-city me-2 text-primary"></i>${msg.text("owner.city")}
                                </label>
                                <input type="text" class="form-control ${validationErrors != null && validationErrors.containsKey("city") ? "is-invalid" : ""}" id="city" name="city"
                                       value="${owner != null && owner.city() != null ? owner.city() : ""}" required>
                                <div class="invalid-feedback">${validationErrors != null && validationErrors.containsKey("city") ? validationErrors.get("city") : msg.text("validation.city.required")}</div>
                            </div>
                            <div class="col-md-6">
                                <label for="telephone" class="form-label">
                                    <i class="fas fa-phone me-2 text-primary"></i>${msg.text("owner.telephone")}
                                </label>
                                <input type="tel" class="form-control ${validationErrors != null && validationErrors.containsKey("telephone") ? "is-invalid" : ""}" id="telephone" name="telephone"
                                       value="${owner != null && owner.telephone() != null ? owner.telephone() : ""}" pattern="[0-9]{10}" required>
                                <div class="invalid-feedback">${validationErrors != null && validationErrors.containsKey("telephone") ? validationErrors.get("telephone") : msg.text("validation.telephone")}</div>
                            </div>
                        </div>

                        <hr class="my-4">

                        <div class="d-flex gap-3">
                            <button type="submit" class="btn btn-primary btn-lg flex-grow-1">
                                <i class="${isNew ? "fas fa-plus me-2" : "fas fa-save me-2"}"></i>
                                ${isNew ? msg.text("button.addOwner") : msg.text("button.updateOwner")}
                            </button>
                            <a href="${isNew ? "/owners/find" : ("/owners/" + owner.id())}" class="btn btn-secondary btn-lg">
                                <i class="fas fa-times me-2"></i>${msg.text("button.cancel")}
                            </a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request, locale = locale)
