@import io.micronaut.samples.petclinic.dto.ClinicServiceOfferingForm
@import io.micronaut.samples.petclinic.model.Clinic

@param Clinic clinic
@param ClinicServiceOfferingForm service
@param boolean isNew
@param java.util.Map<String, String> validationErrors
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas ${isNew ? "fa-plus" : "fa-pen"} me-3"></i>${isNew ? msg.text("clinic.servicesNew") : msg.text("clinic.servicesEdit")}</h1>
            <p>${clinic.name()} · ${clinic.city()}</p>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <a href="/clinics/${clinic.id()}/services" class="btn btn-secondary mb-4">
                    <i class="fas fa-arrow-left me-2"></i>${msg.text("button.back")}
                </a>

                <div class="form-container animate-fade-in-up">
                    <div class="alert alert-info">
                        <i class="fas fa-circle-info me-2"></i>${msg.text("clinic.servicesFormHelp")}
                    </div>

                    @if(validationErrors != null && !validationErrors.isEmpty())
                        <div class="alert alert-danger" role="alert">${msg.text("validation.fixErrors")}</div>
                    @endif

                    <form id="clinic-service-form"
                          action="${isNew ? "/clinics/" + clinic.id() + "/services" : "/clinics/" + clinic.id() + "/services/" + service.serviceCode()}"
                          data-catalog-url="/clinics/${clinic.id()}/services"
                          data-submit-failed-message="${msg.text("clinic.servicesSaveFailed")}"
                          novalidate>
                        <div id="submit-error" class="alert alert-danger d-none" role="alert"></div>

                        <div class="mb-4">
                            <label for="serviceCode" class="form-label"><i class="fas fa-key me-2 text-primary"></i>${msg.text("clinic.servicesCode")}</label>
                            @if(isNew)
                                <input type="text" class="form-control ${validationErrors.containsKey("serviceCode") ? "is-invalid" : ""}" id="serviceCode" name="serviceCode"
                                       value="${service.serviceCode() != null ? service.serviceCode() : ""}" pattern="[A-Z][A-Z0-9_]*" maxlength="40" required>
                            @else
                                <input type="text" class="form-control ${validationErrors.containsKey("serviceCode") ? "is-invalid" : ""}" id="serviceCode" name="serviceCode"
                                       value="${service.serviceCode() != null ? service.serviceCode() : ""}" readonly required>
                            @endif
                            <div class="form-text">${msg.text("clinic.servicesCodeHelp")}</div>
                            @if(validationErrors.containsKey("serviceCode"))
                                <div class="invalid-feedback">${validationErrors.get("serviceCode")}</div>
                            @endif
                        </div>

                        <div class="mb-4">
                            <label for="name" class="form-label"><i class="fas fa-tag me-2 text-primary"></i>${msg.text("clinic.servicesName")}</label>
                            <input type="text" class="form-control ${validationErrors.containsKey("name") ? "is-invalid" : ""}" id="name" name="name"
                                   value="${service.name() != null ? service.name() : ""}" maxlength="100" required>
                            @if(validationErrors.containsKey("name"))
                                <div class="invalid-feedback">${validationErrors.get("name")}</div>
                            @endif
                        </div>

                        <div class="mb-4">
                            <label for="description" class="form-label"><i class="fas fa-align-left me-2 text-primary"></i>${msg.text("clinic.servicesDescription")}</label>
                            <textarea class="form-control ${validationErrors.containsKey("description") ? "is-invalid" : ""}" id="description" name="description" maxlength="500" rows="3">${service.description() != null ? service.description() : ""}</textarea>
                            @if(validationErrors.containsKey("description"))
                                <div class="invalid-feedback">${validationErrors.get("description")}</div>
                            @endif
                        </div>

                        <div class="row mb-4">
                            <div class="col-md-6">
                                <label for="price" class="form-label"><i class="fas fa-dollar-sign me-2 text-primary"></i>${msg.text("clinic.servicesPrice")}</label>
                                <input type="number" class="form-control ${validationErrors.containsKey("price") ? "is-invalid" : ""}" id="price" name="price"
                                       value="${service.price() != null ? service.price().toPlainString() : ""}" min="0" step="0.01" required>
                                @if(validationErrors.containsKey("price"))
                                    <div class="invalid-feedback">${validationErrors.get("price")}</div>
                                @endif
                            </div>
                            <div class="col-md-6">
                                <label for="durationMinutes" class="form-label"><i class="fas fa-clock me-2 text-primary"></i>${msg.text("clinic.servicesDuration")}</label>
                                <input type="number" class="form-control ${validationErrors.containsKey("durationMinutes") ? "is-invalid" : ""}" id="durationMinutes" name="durationMinutes"
                                       value="${service.durationMinutes() != null ? service.durationMinutes().toString() : ""}" min="1" max="480" required>
                                @if(validationErrors.containsKey("durationMinutes"))
                                    <div class="invalid-feedback">${validationErrors.get("durationMinutes")}</div>
                                @endif
                            </div>
                        </div>

                        <div class="d-flex gap-3">
                            <button type="submit" class="btn btn-primary btn-lg flex-grow-1">
                                <i class="fas fa-save me-2"></i>${msg.text("clinic.servicesSave")}
                            </button>
                            <a href="/clinics/${clinic.id()}/services" class="btn btn-secondary btn-lg">${msg.text("button.cancel")}</a>
                        </div>
                    </form>

                    <script>
                        (() => {
                            const form = document.getElementById('clinic-service-form');
                            const submitError = document.getElementById('submit-error');

                            form.addEventListener('submit', async (event) => {
                                event.preventDefault();
                                form.classList.add('was-validated');

                                if (!form.checkValidity()) {
                                    return;
                                }

                                const formData = new FormData(form);
                                const payload = {
                                    serviceCode: formData.get('serviceCode'),
                                    name: formData.get('name'),
                                    description: formData.get('description'),
                                    price: Number(formData.get('price')),
                                    durationMinutes: Number(formData.get('durationMinutes'))
                                };

                                submitError.classList.add('d-none');

                                try {
                                    const response = await fetch(form.action, {
                                        method: 'POST',
                                        headers: {
                                            'Content-Type': 'application/json',
                                            'Accept': 'application/json'
                                        },
                                        body: JSON.stringify(payload)
                                    });

                                    if (!response.ok) {
                                        let message = form.dataset.submitFailedMessage;
                                        try {
                                            const errorBody = await response.json();
                                            message = errorBody.message || errorBody.error || message;
                                            if (errorBody.errors) {
                                                message += ' ' + Object.values(errorBody.errors).join(' ');
                                            }
                                        } catch (ignored) {
                                            // Keep the localized fallback for non-JSON server errors.
                                        }
                                        submitError.textContent = message;
                                        submitError.classList.remove('d-none');
                                        return;
                                    }

                                    await response.json();
                                    window.location.assign(form.dataset.catalogUrl);
                                } catch (error) {
                                    submitError.textContent = form.dataset.submitFailedMessage;
                                    submitError.classList.remove('d-none');
                                }
                            });
                        })();
                    </script>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
