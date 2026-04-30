@import io.micronaut.samples.petclinic.model.Owner
@import io.micronaut.samples.petclinic.model.Pet
@import java.util.Collection

@param Collection<Owner> owners
@param String lastName
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-users me-3"></i>${msg.text("owner.petOwners")}</h1>
            <p>${msg.text("owner.ownersFound", owners != null ? owners.size() : 0)}</p>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <a href="/owners/find" class="btn btn-secondary">
                <i class="fas fa-arrow-left me-2"></i>${msg.text("owner.backToSearch")}
            </a>
            <a href="/owners/new" class="btn btn-success">
                <i class="fas fa-plus me-2"></i>${msg.text("search.addOwner")}
            </a>
        </div>

        @if(owners == null || owners.isEmpty())
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-users-slash"></i>
                </div>
                <h3>${msg.text("owner.noOwnersFound")}</h3>
                <p>${msg.text("owner.noOwnersMessage")}</p>
                <a href="/owners/new" class="btn btn-success">
                    <i class="fas fa-plus me-2"></i>${msg.text("owner.addNew")}
                </a>
            </div>
        @else
            <div class="row g-4">
                @for(Owner owner : owners)
                    <div class="col-md-6 col-lg-4">
                        <div class="owner-card animate-fade-in-up">
                            <div class="d-flex align-items-start mb-3">
                                <div class="owner-avatar me-3">
                                    <span>
                                        ${owner.getFirstName() != null && owner.getFirstName().length() > 0 ? owner.getFirstName().substring(0, 1) : "?"}
                                        ${owner.getLastName() != null && owner.getLastName().length() > 0 ? owner.getLastName().substring(0, 1) : "?"}
                                    </span>
                                </div>
                                <div class="flex-grow-1">
                                    <h5 class="mb-1">
                                        <a href="/owners/${owner.getId()}" class="text-decoration-none text-dark">
                                            ${owner.getFirstName()} ${owner.getLastName()}
                                        </a>
                                    </h5>
                                    <p class="text-muted mb-0 small">
                                        <i class="fas fa-map-marker-alt me-1"></i>${owner.getCity()}
                                    </p>
                                </div>
                            </div>

                            <div class="mb-3">
                                <p class="mb-1 small text-muted">
                                    <i class="fas fa-home me-2"></i>${owner.getAddress()}
                                </p>
                                <p class="mb-0 small text-muted">
                                    <i class="fas fa-phone me-2"></i>${owner.getTelephone()}
                                </p>
                            </div>

                            @if(owner.getPets() != null && !owner.getPets().isEmpty())
                                <div class="mb-3">
                                    <div class="d-flex flex-wrap">
                                        @for(Pet pet : owner.getPets())
                                            <span class="pet-badge">
                                                <i class="fas fa-paw"></i>
                                                ${pet.getName()}
                                            </span>
                                        @endfor
                                    </div>
                                </div>
                            @else
                                <div class="mb-3">
                                    <span class="text-muted small"><i class="fas fa-info-circle me-1"></i>${msg.text("owner.noPetsRegistered")}</span>
                                </div>
                            @endif

                            <a href="/owners/${owner.getId()}" class="btn btn-primary btn-sm w-100">
                                <i class="fas fa-eye me-2"></i>${msg.text("owner.viewDetails")}
                            </a>
                        </div>
                    </div>
                @endfor
            </div>
        @endif
    </main>
`, msg = msg, request = request)
