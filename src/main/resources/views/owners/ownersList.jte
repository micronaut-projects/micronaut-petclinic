@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-users me-3"></i>${msg.text("owner.petOwners")}</h1>
            <p data-owner-total>${msg.text("owner.listDescription")}</p>
        </div>
    </div>

    <div class="container flex-grow-1 py-4" data-owner-list data-endpoint="/owners/data"
         data-total-template="${msg.text("owner.ownersFound", "__total__")}"
         data-range-template="${msg.text("owner.resultRange", "__start__", "__end__", "__total__")}"
         data-page-template="${msg.text("owner.pageNumber", "__page__", "__pages__")}"
         data-empty-message="${msg.text("owner.emptyPage")}" data-last-name-label="${msg.text("owner.lastName")}"
         data-previous-label="${msg.text("owner.previous")}" data-next-label="${msg.text("owner.next")}"
         data-loading-message="${msg.text("owner.loading")}" data-failure-message="${msg.text("owner.loadFailed")}">
        <div class="d-flex flex-wrap gap-3 justify-content-between align-items-center mb-4">
            <a href="/owners/find" class="btn btn-secondary">
                <i class="fas fa-arrow-left me-2"></i>${msg.text("owner.backToSearch")}
            </a>
            <a href="/owners/new" class="btn btn-success">
                <i class="fas fa-plus me-2"></i>${msg.text("search.addOwner")}
            </a>
        </div>

        <form id="owner-list-controls" action="/owners/list" method="get" class="owner-list-toolbar mb-4" data-owner-controls>
            <input type="hidden" name="lastName" value="">
            <div class="row g-3 align-items-end">
                <div class="col-sm-6 col-lg-4">
                    <label for="owner-sort" class="form-label">${msg.text("owner.sortBy")}</label>
                    <select id="owner-sort" name="sort" class="form-select">
                        @for(String field : java.util.List.of("lastName", "firstName", "city", "id"))
                            <option value="${field}">${msg.text("owner." + field)}</option>
                        @endfor
                    </select>
                </div>
                <div class="col-sm-6 col-lg-3">
                    <label for="owner-direction" class="form-label">${msg.text("owner.sortOrder")}</label>
                    <select id="owner-direction" name="direction" class="form-select">
                        <option value="asc">${msg.text("owner.ascending")}</option>
                        <option value="desc">${msg.text("owner.descending")}</option>
                    </select>
                </div>
                <div class="col-sm-6 col-lg-3">
                    <label for="owner-size" class="form-label">${msg.text("owner.perPage")}</label>
                    <input id="owner-size" name="size" type="number" min="1" max="100" value="5" class="form-control" required>
                </div>
                <div class="col-sm-6 col-lg-2">
                    <button type="submit" class="btn btn-primary w-100 owner-list-apply">
                        <i class="fas fa-check me-2" aria-hidden="true"></i>${msg.text("owner.apply")}
                    </button>
                </div>
            </div>
        </form>
        <div class="owner-list-summary mb-3" data-owner-summary hidden>
            <p class="text-muted small mb-0" data-owner-range></p>
            <span class="owner-filter-label" data-owner-filter hidden></span>
        </div>
        <noscript><p class="alert alert-info">${msg.text("owner.javascriptRequired")}</p></noscript>
        <p class="text-muted small" data-owner-status role="status" hidden></p>
        <div class="alert alert-danger" data-owner-error role="alert" hidden>
            <span data-owner-error-message></span>
            <button type="button" class="btn btn-sm btn-outline-danger ms-2" data-owner-retry>${msg.text("owner.retry")}</button>
        </div>
        <div data-owner-results aria-live="polite">
            <div class="empty-state" data-owner-empty hidden>
                <div class="empty-state-icon">
                    <i class="fas fa-users-slash"></i>
                </div>
                <h3>${msg.text("owner.noOwnersFound")}</h3>
                <p>${msg.text("owner.noOwnersMessage")}</p>
                <a href="/owners/new" class="btn btn-success">
                    <i class="fas fa-plus me-2"></i>${msg.text("owner.addNew")}
                </a>
            </div>
            <div class="row g-4 owner-list-grid" data-owner-grid></div>
        </div>
        <nav aria-label="${msg.text("owner.pagination")}" class="owner-pagination mt-4" data-owner-pagination hidden></nav>
        <template data-owner-card-template>
            <div class="col-md-6 col-lg-4">
                <div class="owner-card">
                    <div class="d-flex align-items-start mb-3">
                        <div class="owner-avatar me-3"><span data-owner-initials></span></div>
                        <div class="flex-grow-1">
                            <h5 class="mb-1"><a data-owner-link data-owner-name class="text-decoration-none text-dark"></a></h5>
                            <p class="text-muted mb-0 small"><i class="fas fa-map-marker-alt me-1" aria-hidden="true"></i><span data-owner-city></span></p>
                        </div>
                    </div>
                    <div class="mb-3">
                        <p class="mb-1 small text-muted"><i class="fas fa-home me-2" aria-hidden="true"></i><span data-owner-address></span></p>
                        <p class="mb-0 small text-muted"><i class="fas fa-phone me-2" aria-hidden="true"></i><span data-owner-phone></span></p>
                    </div>
                    <div class="mb-3 d-flex flex-wrap" data-owner-pets></div>
                    <div class="mb-3" data-owner-no-pets>
                        <span class="text-muted small"><i class="fas fa-info-circle me-1" aria-hidden="true"></i>${msg.text("owner.noPetsRegistered")}</span>
                    </div>
                    <a data-owner-link class="btn btn-primary btn-sm w-100"><i class="fas fa-eye me-2" aria-hidden="true"></i>${msg.text("owner.viewDetails")}</a>
                </div>
            </div>
        </template>
    </div>
    <script src="/resources/js/owner-list.js" defer></script>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
