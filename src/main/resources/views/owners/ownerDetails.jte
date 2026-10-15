@import io.micronaut.samples.petclinic.model.Owner
@import io.micronaut.samples.petclinic.model.Pet
@import io.micronaut.samples.petclinic.model.Visit
@import java.util.Locale

@param Owner owner
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames

@template.layout(content = @`
<div class="page-header">
        <div class="container">
            <div class="d-flex align-items-center">
                <div class="detail-avatar me-4">
                    <span>
                        <span data-owner-field="initials">${owner.getFirstName() != null && owner.getFirstName().length() > 0 ? owner.getFirstName().substring(0, 1) : "?"}${owner.getLastName() != null && owner.getLastName().length() > 0 ? owner.getLastName().substring(0, 1) : "?"}</span>
                    </span>
                </div>
                <div>
                    <h1 data-owner-field="displayName">${owner.getFirstName()} ${owner.getLastName()}</h1>
                    <p class="mb-0"><i class="fas fa-map-marker-alt me-2"></i><span data-owner-field="city">${owner.getCity()}</span></p>
                </div>
            </div>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <a href="/owners/find" class="btn btn-secondary mb-4">
            <i class="fas fa-arrow-left me-2"></i>${msg.text("owner.backToSearch")}
        </a>

        @if(activeNames != null && activeNames.contains("oracle"))
            <ul class="nav nav-tabs mb-4" role="tablist" aria-label="${msg.text("owner.details")}">
                <li class="nav-item" role="presentation">
                    <a id="owner-details-tab" class="nav-link active"
                       href="#owner-details-panel" role="tab">
                        ${msg.text("owner.details")}
                    </a>
                </li>
                <li class="nav-item" role="presentation">
                    <a id="care-record-tab" class="nav-link"
                       href="#care-record-panel" role="tab">
                        <i class="fas fa-code me-2"></i>${msg.text("nav.ownerCareRecord")}
                    </a>
                </li>
            </ul>
        @endif

        <div id="owner-details-panel" class="row g-4">
            <div class="col-lg-4">
                <div class="detail-card animate-fade-in-up">
                    <div class="card-header">
                        <h5 class="mb-0"><i class="fas fa-user me-2"></i>${msg.text("owner.information")}</h5>
                    </div>
                    <div class="detail-body">
                        <div class="info-row">
                            <div class="info-label"><i class="fas fa-user me-2"></i>${msg.text("owner.name")}</div>
                            <div class="info-value" data-owner-field="displayName">${owner.getFirstName()} ${owner.getLastName()}</div>
                        </div>
                        <div class="info-row">
                            <div class="info-label"><i class="fas fa-home me-2"></i>${msg.text("owner.address")}</div>
                            <div class="info-value" data-owner-field="address">${owner.getAddress()}</div>
                        </div>
                        <div class="info-row">
                            <div class="info-label"><i class="fas fa-city me-2"></i>${msg.text("owner.city")}</div>
                            <div class="info-value" data-owner-field="city">${owner.getCity()}</div>
                        </div>
                        <div class="info-row">
                            <div class="info-label"><i class="fas fa-phone me-2"></i>${msg.text("owner.phone")}</div>
                            <div class="info-value" data-owner-field="telephone">${owner.getTelephone()}</div>
                        </div>

                        <div class="d-flex gap-2 mt-4">
                            <a href="/owners/${owner.id()}/edit" class="btn btn-primary flex-grow-1">
                                <i class="fas fa-edit me-2"></i>${msg.text("button.edit")}
                            </a>
                            <a href="/owners/${owner.id()}/pets/new" class="btn btn-success flex-grow-1">
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
                                <a href="/owners/${owner.id()}/pets/new" class="btn btn-success">
                                    <i class="fas fa-plus me-2"></i>${msg.text("owner.addFirstPet")}
                                </a>
                            </div>
                        @else
                            @for(Pet pet : owner.getPets())
                                <div class="pet-card mb-3" data-pet-id="${pet.id()}">
                                    <div class="row align-items-center">
                                        <div class="col-md-4">
                                            <div class="d-flex align-items-center">
                                                <div class="pet-icon me-3 ${
                                                    ((pet.getType() != null && pet.getType().name() != null) ? pet.getType().name().trim().toLowerCase(Locale.ROOT) : "dog")
                                                }">
                                                    <i class="fas ${
                                                        switch (((pet.getType() != null && pet.getType().name() != null) ? pet.getType().name().trim().toLowerCase(Locale.ROOT) : "")) {
                                                            case "cat" -> "fa-cat";
                                                            case "bird" -> "fa-dove";
                                                            case "hamster" -> "fa-hippo";
                                                            case "snake" -> "fa-worm";
                                                            default -> "fa-dog";
                                                        }
                                                    }"></i>
                                                </div>
                                                <div>
                                                    <h6 class="mb-0" data-pet-name>${pet.name()}</h6>
                                                    <small class="text-muted">${pet.getType() != null ? pet.getType().name() : msg.text("visit.unknown")}</small>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-md-3">
                                            <small class="text-muted d-block">${msg.text("pet.birthDate")}</small>
                                            <span data-pet-birth-date>${pet.getBirthDate() != null ? pet.getBirthDate().toString() : ""}</span>
                                        </div>
                                        <div class="col-md-5 text-md-end mt-3 mt-md-0">
                                            <a href="/owners/${owner.id()}/pets/${pet.id()}/edit" class="btn btn-sm btn-outline-primary me-2">
                                                <i class="fas fa-edit"></i> ${msg.text("button.edit")}
                                            </a>
                                            <a href="/owners/${owner.id()}/pets/${pet.id()}/visits/new" class="btn btn-sm btn-primary">
                                                <i class="fas fa-plus"></i> ${msg.text("button.addVisit")}
                                            </a>
                                        </div>
                                    </div>

                                    @if(pet.getVisits() != null && !pet.getVisits().isEmpty())
                                        <div class="mt-3 pt-3 border-top">
                                            <h6 class="text-muted mb-3"><i class="fas fa-calendar-alt me-2"></i>${msg.text("pet.visitHistory")}</h6>
                                            <div class="visit-timeline">
                                                @for(Visit visit : pet.getVisits())
                                                    <div class="visit-item" data-visit-id="${visit.id()}">
                                                        <div class="visit-date" data-visit-date>${visit.date() != null ? visit.date().toString() : ""}</div>
                                                        <div class="visit-description" data-visit-description>${visit.description()}</div>
                                                        <div class="visit-meta">
                                                            <span><i class="fas fa-clock me-1"></i>${visit.duration() != null ? visit.duration().toMinutes() + " " + msg.text("visit.minutes") : ""}</span>
                                                            <span><i class="fas fa-calendar-check me-1"></i>${visit.period() != null ? visit.period().toTotalMonths() + " " + msg.text("visit.months") : ""}</span>
                                                        </div>
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
        @if(activeNames != null && activeNames.contains("oracle"))
            <div id="care-record-panel" hidden>
            <div class="mb-4">
                <h2 class="h3 mb-1">${msg.text("jsonDuality.title")}</h2>
                <p class="text-muted mb-0">${msg.text("jsonDuality.subtitle")}</p>
            </div>
            <div id="care-record-page" data-document-url="/owners/${owner.id()}/care-record"
                 data-loading-label="${msg.text("jsonDuality.loading")}" data-load-error-label="${msg.text("jsonDuality.loadError")}"
                 data-save-label="${msg.text("jsonDuality.saveSuccess")}" data-save-error-label="${msg.text("jsonDuality.saveError")}"
                 data-conflict-label="${msg.text("jsonDuality.conflict")}" data-invalid-label="${msg.text("jsonDuality.invalid")}">
                <div class="row g-4">
                    <div class="col-lg-4">
                        <div class="card animate-fade-in-up">
                            <div class="card-header"><h5 class="mb-0">${msg.text("jsonDuality.howItWorks")}</h5></div>
                            <div class="card-body">
                                <p>${msg.text("jsonDuality.explanation")}</p>
                                <ul class="mb-0">
                                    <li><code>@JsonView</code> ${msg.text("jsonDuality.ownerMapping")}</li>
                                    <li><code>@JsonSubView</code> ${msg.text("jsonDuality.nestedMapping")}</li>
                                    <li><code>@Relation</code> ${msg.text("jsonDuality.collectionMapping")}</li>
                                    <li>${msg.text("jsonDuality.repositoryMapping")}</li>
                                </ul>
                            </div>
                        </div>
                    </div>

                    <div class="col-lg-8">
                        <div class="card animate-fade-in-up stagger-1">
                            <div class="card-header d-flex justify-content-between align-items-center">
                                <h5 class="mb-0"><i class="fas fa-code me-2"></i>${msg.text("jsonDuality.document")}</h5>
                                <span class="badge bg-secondary">${msg.text("jsonDuality.oracleOnly")}</span>
                            </div>
                            <div class="card-body">
                                <p class="text-muted">${msg.text("jsonDuality.editInstructions")}</p>
                                <div id="document-status" class="mt-3" role="status" aria-live="polite" hidden></div>
                                <textarea id="owner-document" class="form-control font-monospace" rows="25"
                                          spellcheck="false" aria-label="${msg.text("jsonDuality.document")}"></textarea>
                                <div class="d-flex flex-wrap gap-2 mt-3">
                                    <button id="reload-document" class="btn btn-outline-secondary" type="button">
                                        <i class="fas fa-rotate me-2"></i>${msg.text("jsonDuality.reload")}
                                    </button>
                                    <button id="save-document" class="btn btn-primary" type="button">
                                        <i class="fas fa-floppy-disk me-2"></i>${msg.text("jsonDuality.save")}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            </div>

            <script>
                (() => {
                    const page = document.getElementById("care-record-page");
                    const detailsPanel = document.getElementById("owner-details-panel");
                    const carePanel = document.getElementById("care-record-panel");
                    const detailsTab = document.getElementById("owner-details-tab");
                    const careTab = document.getElementById("care-record-tab");
                    const editor = document.getElementById("owner-document");
                    const status = document.getElementById("document-status");
                    const documentUrl = page.dataset.documentUrl;
                    let documentLoaded = false;
                    let documentLoading = false;
                    const showPanel = (careRecord) => {
                        detailsPanel.hidden = careRecord;
                        carePanel.hidden = !careRecord;
                        detailsTab.classList.toggle("active", !careRecord);
                        careTab.classList.toggle("active", careRecord);
                    };
                    const setStatus = (message, style = "info") => {
                        const hasMessage = Boolean(message);
                        status.hidden = !hasMessage;
                        status.className = hasMessage ? "mt-3 alert alert-" + style : "mt-3";
                        status.textContent = hasMessage ? message : "";
                    };
                    const loadDocument = async (force = false) => {
                        if ((!force && documentLoaded) || documentLoading) return;
                        documentLoading = true;
                        setStatus(page.dataset.loadingLabel, "secondary");
                        try {
                            const response = await fetch(documentUrl, { headers: { Accept: "application/json" } });
                            if (!response.ok) throw new Error(page.dataset.loadErrorLabel);
                            editor.value = JSON.stringify(await response.json(), null, 2);
                            documentLoaded = true;
                            setStatus("");
                        } catch (error) {
                            setStatus(error.message || page.dataset.loadErrorLabel, "danger");
                        } finally {
                            documentLoading = false;
                        }
                    };
                    const updateOwnerDetails = (updatedDocument) => {
                        const displayName = (updatedDocument.firstName || "") + " " + (updatedDocument.lastName || "");
                        window.document.querySelectorAll('[data-owner-field="displayName"]').forEach((element) => {
                            element.textContent = displayName;
                        });
                        window.document.querySelectorAll('[data-owner-field="initials"]').forEach((element) => {
                            element.textContent = (updatedDocument.firstName?.[0] || "?") + (updatedDocument.lastName?.[0] || "?");
                        });
                        for (const field of ["address", "city", "telephone"]) {
                            window.document.querySelectorAll('[data-owner-field="' + field + '"]').forEach((element) => {
                                element.textContent = updatedDocument[field] || "";
                            });
                        }
                        for (const pet of updatedDocument.pets || []) {
                            const petCard = detailsPanel.querySelector('[data-pet-id="' + pet.id + '"]');
                            if (!petCard) continue;
                            petCard.querySelector("[data-pet-name]").textContent = pet.name || "";
                            petCard.querySelector("[data-pet-birth-date]").textContent = (pet.birthDate || "").slice(0, 10);
                            for (const visit of pet.visits || []) {
                                const visitItem = petCard.querySelector('[data-visit-id="' + visit.id + '"]');
                                if (!visitItem) continue;
                                visitItem.querySelector("[data-visit-date]").textContent = (visit.date || "").slice(0, 10);
                                visitItem.querySelector("[data-visit-description]").textContent = visit.description || "";
                            }
                        }
                    };
                    const updateTabUrl = (tab) => {
                        const url = new URL(window.location.href);
                        url.searchParams.set("tab", tab);
                        window.history.replaceState(null, "", url);
                    };
                    document.getElementById("reload-document").addEventListener("click", () => loadDocument(true));
                    detailsTab.addEventListener("click", (event) => {
                        event.preventDefault();
                        showPanel(false);
                        updateTabUrl("owner-details");
                    });
                    careTab.addEventListener("click", (event) => {
                        event.preventDefault();
                        showPanel(true);
                        updateTabUrl("care-record");
                        void loadDocument();
                    });
                    document.getElementById("save-document").addEventListener("click", async () => {
                        let document;
                        try {
                            document = JSON.parse(editor.value);
                        } catch {
                            setStatus(page.dataset.invalidLabel, "warning");
                            return;
                        }
                        try {
                            const response = await fetch(documentUrl, {
                                method: "PUT",
                                headers: { "Content-Type": "application/json", Accept: "application/json" },
                                body: JSON.stringify(document)
                            });
                            const result = await response.json();
                            if (response.status === 409) {
                                setStatus(result.message || page.dataset.conflictLabel, "warning");
                                return;
                            }
                            if (!response.ok) {
                                setStatus(result.message || page.dataset.saveErrorLabel, "danger");
                                return;
                            }
                            editor.value = JSON.stringify(result, null, 2);
                            updateOwnerDetails(result);
                            setStatus(page.dataset.saveLabel, "success");
                        } catch (error) {
                            setStatus(error.message || page.dataset.saveErrorLabel, "danger");
                        }
                    });
                    const selectedTab = new URLSearchParams(window.location.search).get("tab");
                    const showCareRecord = selectedTab === "care-record";
                    showPanel(showCareRecord);
                    if (showCareRecord) {
                        void loadDocument();
                    }
                })();
            </script>
        @endif
    </main>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
