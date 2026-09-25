@import io.micronaut.samples.petclinic.model.Clinic
@import io.micronaut.samples.petclinic.model.ClinicServiceOffering

@param Clinic clinic
@param java.util.List<ClinicServiceOffering> services
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-hand-holding-medical me-3"></i>${msg.text("clinic.servicesTitle", clinic.name())}</h1>
            <p>${msg.text("clinic.servicesSubtitle")}</p>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-4">
            <div>
                <a href="/clinics" class="btn btn-secondary">
                    <i class="fas fa-arrow-left me-2"></i>${msg.text("clinic.servicesBack")}
                </a>
                <span class="text-muted ms-3"><i class="fas fa-location-dot me-1"></i>${clinic.city()}</span>
            </div>
            <a href="/clinics/${clinic.id()}/services/new" class="btn btn-primary">
                <i class="fas fa-plus me-2"></i>${msg.text("clinic.servicesNew")}
            </a>
        </div>

        <div class="card border-primary mb-4">
            <div class="card-body">
                <h5 class="card-title"><i class="fas fa-arrows-rotate me-2 text-primary"></i>${msg.text("clinic.servicesUpsertTitle")}</h5>
                <p class="card-text mb-0">${msg.text("clinic.servicesUpsertDescription")}</p>
            </div>
        </div>

        @if(services == null || services.isEmpty())
            <div class="empty-state">
                <div class="empty-state-icon"><i class="fas fa-hand-holding-medical"></i></div>
                <h3>${msg.text("clinic.servicesEmpty")}</h3>
                <a href="/clinics/${clinic.id()}/services/new" class="btn btn-primary">
                    <i class="fas fa-plus me-2"></i>${msg.text("clinic.servicesNew")}
                </a>
            </div>
        @else
            <div class="card shadow-sm">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead>
                            <tr>
                                <th>${msg.text("clinic.servicesCode")}</th>
                                <th>${msg.text("clinic.servicesName")}</th>
                                <th>${msg.text("clinic.servicesDescription")}</th>
                                <th>${msg.text("clinic.servicesPrice")}</th>
                                <th>${msg.text("clinic.servicesDuration")}</th>
                                <th class="text-end"></th>
                            </tr>
                        </thead>
                        <tbody>
                            @for(ClinicServiceOffering offering : services)
                                <tr>
                                    <td><code>${offering.serviceCode()}</code></td>
                                    <td class="fw-semibold">${offering.name()}</td>
                                    <td>${offering.description() != null ? offering.description() : "—"}</td>
                                    <td>${offering.price()}</td>
                                    <td>${msg.text("clinic.servicesMinutes", offering.durationMinutes())}</td>
                                    <td class="text-end">
                                        <a href="/clinics/${clinic.id()}/services/${offering.serviceCode()}/edit" class="btn btn-outline-primary btn-sm">
                                            <i class="fas fa-pen me-1"></i>${msg.text("clinic.servicesEdit")}
                                        </a>
                                    </td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>
            </div>
        @endif
    </main>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
