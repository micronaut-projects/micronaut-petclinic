@import io.micronaut.samples.petclinic.model.Owner

@param Owner owner
@param boolean notFound
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-search me-3"></i>${msg.text("owner.findOwners")}</h1>
            <p>${msg.text("search.searchOwnersByLastName")}</p>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="search-box mb-4 animate-fade-in-up">
            <form action="/owners/list" method="get">
                <div class="row g-3 align-items-end">
                    <div class="col-md-8">
                        <label for="lastName" class="form-label">
                            <i class="fas fa-user me-2"></i>${msg.text("search.ownerLastName")}
                        </label>
                        <input type="text" class="form-control form-control-lg" id="lastName" name="lastName"
                               value="${owner != null ? owner.getLastName() : ""}">
                    </div>
                    <div class="col-md-4">
                        <button class="btn btn-primary btn-lg w-100" type="submit">
                            <i class="fas fa-search me-2"></i>${msg.text("button.search")}
                        </button>
                    </div>
                </div>
            </form>
        </div>

        @if(notFound)
            <div class="alert alert-warning animate-fade-in-up">
                <i class="fas fa-exclamation-triangle"></i>
                ${msg.text("search.tryDifferent")}
            </div>
        @endif

        <div class="row g-4 mt-4">
            <div class="col-md-6 animate-fade-in-up stagger-1">
                <div class="card h-100">
                    <div class="card-body d-flex align-items-center">
                        <div class="feature-icon gradient-4 me-4" style="width: 60px; height: 60px; font-size: 1.5rem;">
                            <i class="fas fa-user-plus"></i>
                        </div>
                        <div>
                            <h5 class="mb-1">${msg.text("owner.addNew")}</h5>
                            <p class="text-muted mb-2">${msg.text("form.newOwner.desc")}</p>
                            <a href="/owners/new" class="btn btn-success">
                                <i class="fas fa-plus me-2"></i>${msg.text("button.addOwner")}
                            </a>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-6 animate-fade-in-up stagger-2">
                <div class="card h-100">
                    <div class="card-body d-flex align-items-center">
                        <div class="feature-icon gradient-1 me-4" style="width: 60px; height: 60px; font-size: 1.5rem;">
                            <i class="fas fa-list"></i>
                        </div>
                        <div>
                            <h5 class="mb-1">${msg.text("owner.viewAll")}</h5>
                            <p class="text-muted mb-2">${msg.text("owner.viewAllMessage")}</p>
                            <a href="/owners/list" class="btn btn-primary">
                                <i class="fas fa-eye me-2"></i>${msg.text("owner.viewAll")}
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="card mt-5 animate-fade-in-up stagger-3">
            <div class="card-body">
                <h5 class="mb-3"><i class="fas fa-lightbulb text-warning me-2"></i>${msg.text("search.tips")}</h5>
                <div class="row">
                    <div class="col-md-4">
                        <p class="mb-1"><strong>${msg.text("search.partialMatch.title")}</strong></p>
                        <p class="text-muted small">${msg.text("search.partialMatch.desc")}</p>
                    </div>
                    <div class="col-md-4">
                        <p class="mb-1"><strong>${msg.text("search.emptySearch.title")}</strong></p>
                        <p class="text-muted small">${msg.text("search.emptySearch.desc")}</p>
                    </div>
                    <div class="col-md-4">
                        <p class="mb-1"><strong>${msg.text("search.caseInsensitive.title")}</strong></p>
                        <p class="text-muted small">${msg.text("search.caseInsensitive.desc")}</p>
                    </div>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request)
