@import io.micronaut.samples.petclinic.model.Owner
@import io.micronaut.samples.petclinic.model.Pet
@import io.micronaut.samples.petclinic.model.Visit

@param Owner owner
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <div class="d-flex align-items-center">
                <div class="detail-avatar me-4">
                    <span>
                        ${owner.getFirstName() != null && owner.getFirstName().length() > 0 ? owner.getFirstName().substring(0, 1) : "?"}
                        ${owner.getLastName() != null && owner.getLastName().length() > 0 ? owner.getLastName().substring(0, 1) : "?"}
                    </span>
                </div>
                <div>
                    <h1>${owner.getFirstName()} ${owner.getLastName()}</h1>
                    <p class="mb-0"><i class="fas fa-map-marker-alt me-2"></i>${owner.getCity()}</p>
                </div>
            </div>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <a href="/owners/find" class="btn btn-secondary mb-4">
            <i class="fas fa-arrow-left me-2"></i>${msg.text("owner.backToSearch")}
        </a>

        <div class="row g-4">
            <div class="col-lg-4">
                <div class="detail-card animate-fade-in-up">
                    <div class="card-header">
                        <h5 class="mb-0"><i class="fas fa-user me-2"></i>${msg.text("owner.information")}</h5>
                    </div>
                    <div class="detail-body">
                        <div class="info-row">
                            <div class="info-label"><i class="fas fa-user me-2"></i>${msg.text("owner.name")}</div>
                            <div class="info-value">${owner.getFirstName()} ${owner.getLastName()}</div>
                        </div>
                        <div class="info-row">
                            <div class="info-label"><i class="fas fa-home me-2"></i>${msg.text("owner.address")}</div>
                            <div class="info-value">${owner.getAddress()}</div>
                        </div>
                        <div class="info-row">
                            <div class="info-label"><i class="fas fa-city me-2"></i>${msg.text("owner.city")}</div>
                            <div class="info-value">${owner.getCity()}</div>
                        </div>
                        <div class="info-row">
                            <div class="info-label"><i class="fas fa-phone me-2"></i>${msg.text("owner.phone")}</div>
                            <div class="info-value">${owner.getTelephone()}</div>
                        </div>

                        <div class="d-flex gap-2 mt-4">
                            <a href="/owners/${owner.getId()}/edit" class="btn btn-primary flex-grow-1">
                                <i class="fas fa-edit me-2"></i>${msg.text("button.edit")}
                            </a>
                            <a href="/owners/${owner.getId()}/pets/new" class="btn btn-success flex-grow-1">
                                <i class="fas fa-plus me-2"></i>${msg.text("button.addPet")}
                            </a>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-lg-8">
                <div class="card animate-fade-in-up stagger-1">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><i class="fas fa-paw me-2"></i>${msg.text("owner.petsAndVisits")}</h5>
                        <span class="badge bg-white text-primary">
                            ${msg.text("pets.count", owner.getPets() != null ? owner.getPets().size() : 0)}
                        </span>
                    </div>
                    <div class="card-body">
                        @if(owner.getPets() == null || owner.getPets().isEmpty())
                            <div class="text-center py-5">
                                <div class="empty-state-icon mx-auto mb-3" style="width: 80px; height: 80px; font-size: 2rem;">
                                    <i class="fas fa-paw"></i>
                                </div>
                                <h5>${msg.text("owner.noPetsYet")}</h5>
                                <p class="text-muted">${msg.text("owner.noPetsMessage")}</p>
                                <a href="/owners/${owner.getId()}/pets/new" class="btn btn-success">
                                    <i class="fas fa-plus me-2"></i>${msg.text("owner.addFirstPet")}
                                </a>
                            </div>
                        @else
                            @for(Pet pet : owner.getPets())
                                <div class="pet-card mb-3">
                                    <div class="row align-items-center">
                                        <div class="col-md-4">
                                            <div class="d-flex align-items-center">
                                                <div class="pet-icon me-3 dog">
                                                    <i class="fas fa-paw"></i>
                                                </div>
                                                <div>
                                                    <h6 class="mb-0">${pet.getName()}</h6>
                                                    <small class="text-muted">${pet.getType() != null ? pet.getType().getName() : msg.text("visit.unknown")}</small>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-md-3">
                                            <small class="text-muted d-block">${msg.text("pet.birthDate")}</small>
                                            <span>${pet.getBirthDate() != null ? pet.getBirthDate().toString() : ""}</span>
                                        </div>
                                        <div class="col-md-5 text-md-end mt-3 mt-md-0">
                                            <a href="/owners/${owner.getId()}/pets/${pet.getId()}/edit" class="btn btn-sm btn-outline-primary me-2">
                                                <i class="fas fa-edit"></i> ${msg.text("button.edit")}
                                            </a>
                                            <a href="/owners/${owner.getId()}/pets/${pet.getId()}/visits/new" class="btn btn-sm btn-primary">
                                                <i class="fas fa-plus"></i> ${msg.text("button.addVisit")}
                                            </a>
                                        </div>
                                    </div>

                                    @if(pet.getVisits() != null && !pet.getVisits().isEmpty())
                                        <div class="mt-3 pt-3 border-top">
                                            <h6 class="text-muted mb-3"><i class="fas fa-calendar-alt me-2"></i>${msg.text("pet.visitHistory")}</h6>
                                            <div class="visit-timeline">
                                                @for(Visit visit : pet.getVisits())
                                                    <div class="visit-item">
                                                        <div class="visit-date">${visit.getDate() != null ? visit.getDate().toString() : ""}</div>
                                                        <div class="visit-description">${visit.getDescription()}</div>
                                                    </div>
                                                @endfor
                                            </div>
                                        </div>
                                    @endif
                                </div>
                            @endfor
                        @endif
                    </div>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request)
