@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames
@param java.util.List<io.micronaut.samples.petclinic.model.Speciality> specialities

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-list me-3"></i>${msg.text("speciality.title")}</h1>
            <p>${msg.text("speciality.description")}</p>
        </div>
    </div>
    <main class="container flex-grow-1 py-4">
        <div class="d-flex gap-2 flex-wrap mb-4">
            <a href="/vets/specialities/new" class="btn btn-primary">
                <i class="fas fa-plus me-1"></i>${msg.text("speciality.add")}
            </a>
            <a href="/vets" class="btn btn-outline-secondary">${msg.text("vet.backToVets")}</a>
        </div>
        @if(specialities.isEmpty())
            <div class="empty-state">
                <div class="empty-state-icon"><i class="fas fa-list"></i></div>
                <h2>${msg.text("speciality.empty")}</h2>
            </div>
        @else
            <div class="card">
                <div class="table-responsive">
                    <table class="table table-hover mb-0">
                        <thead><tr><th scope="col">${msg.text("speciality.name")}</th><th scope="col">${msg.text("speciality.action")}</th></tr></thead>
                        <tbody>
                            @for(var speciality : specialities)
                                <tr>
                                    <td>${speciality.name()}</td>
                                    <td><a class="btn btn-sm btn-outline-primary" href="/vets/specialities/${speciality.id()}/edit">
                                        <i class="fas fa-pen me-1"></i>${msg.text("button.edit")}
                                    </a></td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>
            </div>
        @endif
    </main>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
