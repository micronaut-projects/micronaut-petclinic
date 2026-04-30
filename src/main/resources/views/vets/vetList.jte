@param java.util.List<io.micronaut.samples.petclinic.model.Vet> vets
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-user-md me-3"></i>${msg.text("vet.ourVeterinarians")}</h1>
            <p>${msg.text("vet.meetTeam")}</p>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="row g-4 mb-5">
            <div class="col-md-4">
                <div class="card h-100">
                    <div class="card-body d-flex align-items-center">
                        <div class="feature-icon gradient-1 me-3" style="width: 50px; height: 50px; font-size: 1.25rem;">
                            <i class="fas fa-user-md"></i>
                        </div>
                        <div>
                            <h3 class="mb-0">${vets != null ? vets.size() : 0}</h3>
                            <p class="text-muted mb-0">${msg.text("vet.veterinarians")}</p>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card h-100">
                    <div class="card-body d-flex align-items-center">
                        <div class="feature-icon gradient-2 me-3" style="width: 50px; height: 50px; font-size: 1.25rem;">
                            <i class="fas fa-award"></i>
                        </div>
                        <div>
                            <h3 class="mb-0">${msg.text("vet.expert")}</h3>
                            <p class="text-muted mb-0">${msg.text("vet.careQuality")}</p>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card h-100">
                    <div class="card-body d-flex align-items-center">
                        <div class="feature-icon gradient-4 me-3" style="width: 50px; height: 50px; font-size: 1.25rem;">
                            <i class="fas fa-clock"></i>
                        </div>
                        <div>
                            <h3 class="mb-0">24/7</h3>
                            <p class="text-muted mb-0">${msg.text("vet.availability")}</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        @if (vets == null || vets.isEmpty())
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-user-md"></i>
                </div>
                <h3>${msg.text("vet.noVeterinarians")}</h3>
                <p>${msg.text("vet.noVeterinariansMessage")}</p>
            </div>
        @else
            <div class="row g-4">
                @for (io.micronaut.samples.petclinic.model.Vet vet : vets)
                    <div class="col-md-6 col-lg-4">
                        <div class="vet-card animate-fade-in-up">
                            <div class="vet-card-header">
                                <div class="vet-avatar">
                                    <i class="fas fa-user-md"></i>
                                </div>
                            </div>
                            <div class="vet-card-body">
                                <h5>${vet.getFirstName()} ${vet.getLastName()}</h5>

                                @if (vet.getSpecialties() != null && !vet.getSpecialties().isEmpty())
                                    <div class="mt-3">
                                        @for (io.micronaut.samples.petclinic.model.Specialty s : vet.getSpecialties())
                                            <span class="specialty-badge">${s.getName()}</span>
                                        @endfor
                                    </div>
                                @else
                                    <div class="mt-3">
                                        <span class="specialty-badge">${msg.text("vet.generalPractice")}</span>
                                    </div>
                                @endif

                                <div class="mt-4 pt-3 border-top">
                                    <div class="d-flex justify-content-center gap-3">
                                        <span class="text-muted small">
                                            <i class="fas fa-certificate text-warning me-1"></i>${msg.text("vet.licensed")}
                                        </span>
                                        <span class="text-muted small">
                                            <i class="fas fa-star text-warning me-1"></i>${msg.text("vet.topRated")}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                @endfor
            </div>
        @endif
    </main>
`, msg = msg, request = request)
