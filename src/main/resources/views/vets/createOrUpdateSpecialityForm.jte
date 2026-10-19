@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames
@param io.micronaut.samples.petclinic.dto.SpecialityDto specialityForm
@param boolean isNew
@param boolean saved
@param boolean duplicate

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-list me-3"></i>${isNew ? msg.text("speciality.add") : msg.text("speciality.edit")}</h1>
            <p>${msg.text("speciality.formHelp")}</p>
        </div>
    </div>
    <main class="container flex-grow-1 py-4">
        @if(saved)
            <div id="speciality-saved-alert" class="alert alert-success" role="status">${msg.text("speciality.saved")}</div>
        @endif
        @if(duplicate)
            <div class="alert alert-warning" role="alert">${msg.text("speciality.duplicate")}</div>
        @endif
        <div id="speciality-feedback" class="alert" role="status" aria-live="polite" hidden
             data-saved="${msg.text("speciality.saved")}"
             data-duplicate="${msg.text("speciality.duplicate")}"
             data-error="${msg.text("speciality.requestFailed")}"
             data-network-error="${msg.text("speciality.networkError")}"></div>
        <section class="card">
            <div class="card-body">
                <form id="speciality-form" method="post" action="${isNew ? "/vets/specialities" : "/vets/specialities/" + specialityForm.id()}">
                    @if(!isNew)
                        <input type="hidden" name="id" value="${specialityForm.id()}">
                    @endif
                    <div class="mb-3">
                        <label for="speciality-name" class="form-label">${msg.text("speciality.name")}</label>
                        <input id="speciality-name" name="name" class="form-control" maxlength="80" required
                               value="${specialityForm.name() != null ? specialityForm.name() : ""}">
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary">
                            <i class="fas fa-floppy-disk me-1"></i>${msg.text("button.save")}
                        </button>
                        <a href="/vets/specialities" class="btn btn-outline-secondary">${msg.text("button.cancel")}</a>
                    </div>
                </form>
            </div>
        </section>
    </main>
    <script src="/resources/js/speciality-form.js" defer></script>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
