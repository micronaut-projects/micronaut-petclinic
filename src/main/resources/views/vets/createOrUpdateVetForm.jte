@import io.micronaut.samples.petclinic.dto.VetForm

@param VetForm vet
@param java.util.Map<String, String> validationErrors
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1>
                <i class="fas fa-user-plus me-3"></i>
                ${msg.text("vet.new")}
            </h1>
            <p>${msg.text("form.newVet.desc")}</p>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <a href="/vets" class="btn btn-secondary mb-4">
                    <i class="fas fa-arrow-left me-2"></i>${msg.text("button.back")}
                </a>

                <div class="form-container animate-fade-in-up">
                    @if(validationErrors != null && !validationErrors.isEmpty())
                        <div class="alert alert-danger" role="alert">
                            ${msg.text("validation.fixErrors")}
                        </div>
                    @endif
                    <form action="/vets/new" method="post" class="needs-validation" novalidate>
                        <div class="row mb-4">
                            <div class="col-md-6 mb-3 mb-md-0">
                                <label for="firstName" class="form-label">
                                    <i class="fas fa-user-md me-2 text-primary"></i>${msg.text("vet.firstName")}
                                </label>
                                <input type="text" class="form-control ${validationErrors != null && validationErrors.containsKey("firstName") ? "is-invalid" : ""}" id="firstName" name="firstName"
                                       value="${vet != null && vet.firstName() != null ? vet.firstName() : ""}" required>
                                <div class="invalid-feedback">${validationErrors != null && validationErrors.containsKey("firstName") ? validationErrors.get("firstName") : msg.text("validation.firstName.required")}</div>
                            </div>
                            <div class="col-md-6">
                                <label for="lastName" class="form-label">
                                    <i class="fas fa-user-md me-2 text-primary"></i>${msg.text("vet.lastName")}
                                </label>
                                <input type="text" class="form-control ${validationErrors != null && validationErrors.containsKey("lastName") ? "is-invalid" : ""}" id="lastName" name="lastName"
                                       value="${vet != null && vet.lastName() != null ? vet.lastName() : ""}" required>
                                <div class="invalid-feedback">${validationErrors != null && validationErrors.containsKey("lastName") ? validationErrors.get("lastName") : msg.text("validation.lastName.required")}</div>
                            </div>
                        </div>

                        <hr class="my-4">

                        <div class="d-flex gap-3">
                            <button type="submit" class="btn btn-primary btn-lg flex-grow-1">
                                <i class="fas fa-plus me-2"></i>${msg.text("button.addVet")}
                            </button>
                            <a href="/vets" class="btn btn-secondary btn-lg">
                                <i class="fas fa-times me-2"></i>${msg.text("button.cancel")}
                            </a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
