@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames
@param io.micronaut.samples.petclinic.dto.VetDto vetForm
@param java.util.List<io.micronaut.samples.petclinic.model.Speciality> specialities
@param boolean isNew
@param boolean conflict
@param boolean saved

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-user-md me-3"></i>${isNew ? msg.text("vet.add") : msg.text("vet.edit")}</h1>
            <p>${msg.text("vet.formHelp")}</p>
        </div>
    </div>
    <main class="container flex-grow-1 py-4">
        @if(conflict)
            <div class="alert alert-warning" role="alert">${msg.text("vet.conflict")}</div>
        @endif
        @if(saved)
            <div class="alert alert-success" role="status">${msg.text("vet.saved")}</div>
        @endif
        @if(!isNew)
            <div id="vet-feedback" class="alert" role="status" aria-live="polite" hidden
                 data-saved="${msg.text("vet.saved")}"
                 data-conflict="${msg.text("vet.conflictPending")}"
                 data-missing-etag="${msg.text("vet.reloadRequired")}"
                 data-error="${msg.text("vet.requestFailed")}"
                 data-network-error="${msg.text("vet.networkError")}">
                <span id="vet-feedback-message"></span>
                <a id="vet-reload" class="alert-link ms-2" href="/vets/${vetForm.id()}/edit" hidden>${msg.text("vet.reload")}</a>
            </div>
        @endif
        <section class="card">
            <div class="card-body">
                <form id="vet-edit-form" method="post" action="${isNew ? "/vets/new" : "/vets/" + vetForm.id() + "/edit"}">
                    @if(!isNew)
                        <input type="hidden" name="id" value="${vetForm.id()}">
                        <input type="hidden" name="etag" value="${vetForm.etag()}">
                    @endif
                    <div class="row g-3">
                        <div class="col-md-6">
                            <label for="vet-first-name" class="form-label">${msg.text("vet.firstName")}</label>
                            <input id="vet-first-name" name="firstName" class="form-control" maxlength="80" required
                                   value="${vetForm.firstName() != null ? vetForm.firstName() : ""}">
                        </div>
                        <div class="col-md-6">
                            <label for="vet-last-name" class="form-label">${msg.text("vet.lastName")}</label>
                            <input id="vet-last-name" name="lastName" class="form-control" maxlength="80" required
                                   value="${vetForm.lastName() != null ? vetForm.lastName() : ""}">
                        </div>
                        <div class="col-12">
                            <fieldset>
                                <legend class="form-label">${msg.text("vet.specialities")}</legend>
                                @if(specialities.isEmpty())
                                    <p class="text-muted">${msg.text("speciality.noneAvailable")}</p>
                                @else
                                    @for(var speciality : specialities)
                                        <div class="form-check form-check-inline">
                                            @if(vetForm.specialityIds().contains(speciality.id()))
                                                <input id="vet-speciality-${speciality.id()}" name="specialityIds"
                                                       class="form-check-input" type="checkbox" value="${speciality.id()}" checked>
                                            @else
                                                <input id="vet-speciality-${speciality.id()}" name="specialityIds"
                                                       class="form-check-input" type="checkbox" value="${speciality.id()}">
                                            @endif
                                            <label class="form-check-label" for="vet-speciality-${speciality.id()}">
                                                ${speciality.name()}
                                            </label>
                                        </div>
                                    @endfor
                                @endif
                            </fieldset>
                        </div>
                        <div class="col-12 d-flex gap-2 flex-wrap">
                            <button type="submit" class="btn btn-primary">
                                <i class="fas fa-floppy-disk me-1"></i>${msg.text("button.save")}
                            </button>
                            <a href="/vets" class="btn btn-outline-secondary">${msg.text("button.cancel")}</a>
                        </div>
                    </div>
                </form>
                @if(!isNew)
                    <form id="vet-delete-form" method="post" action="/vets/${vetForm.id()}/delete" class="mt-4 pt-3 border-top"
                          data-confirm="${msg.text("vet.confirmDelete")}">
                        <input type="hidden" name="etag" value="${vetForm.etag()}">
                        <button type="submit" class="btn btn-outline-danger">
                            <i class="fas fa-trash me-1"></i>${msg.text("vet.delete")}
                        </button>
                    </form>
                @endif
            </div>
        </section>
    </main>
    @if(!isNew)
        <script src="/resources/js/vet-form.js" defer></script>
    @endif
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
