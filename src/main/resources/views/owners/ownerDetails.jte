@import io.micronaut.samples.petclinic.model.Owner
@import io.micronaut.samples.petclinic.model.Pet
@import io.micronaut.samples.petclinic.model.Visit
@import java.util.Locale

@param Owner owner
@param String activeTab
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

        <ul class="nav nav-tabs mb-4" role="tablist" aria-label="${msg.text("owner.details")}">
            <li class="nav-item" role="presentation">
                <a class="nav-link ${activeNames != null && activeNames.contains("oracle") && "json-duality".equals(activeTab) ? "" : "active"}"
                   href="/owners/${owner.id()}" role="tab">
                    ${msg.text("owner.details")}
                </a>
            </li>
            @if(activeNames != null && activeNames.contains("oracle"))
                <li class="nav-item" role="presentation">
                    <a class="nav-link ${"json-duality".equals(activeTab) ? "active" : ""}"
                       href="/owners/${owner.id()}?tab=json-duality" role="tab">
                        <i class="fas fa-code me-2"></i>${msg.text("nav.jsonDuality")}
                    </a>
                </li>
            @endif
        </ul>

        @if(!(activeNames != null && activeNames.contains("oracle") && "json-duality".equals(activeTab)))
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
                                <div class="pet-card mb-3">
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
                                                    <h6 class="mb-0">${pet.name()}</h6>
                                                    <small class="text-muted">${pet.getType() != null ? pet.getType().name() : msg.text("visit.unknown")}</small>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="col-md-3">
                                            <small class="text-muted d-block">${msg.text("pet.birthDate")}</small>
                                            <span>${pet.getBirthDate() != null ? pet.getBirthDate().toString() : ""}</span>
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
                                                    <div class="visit-item">
                                                        <div class="visit-date">${visit.date() != null ? visit.date().toString() : ""}</div>
                                                        <div class="visit-description">${visit.description()}</div>
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
        @endif

        @if(activeNames != null && activeNames.contains("oracle") && "json-duality".equals(activeTab))
            <div class="mb-4">
                <h2 class="h3 mb-1">${msg.text("jsonDuality.title")}</h2>
                <p class="text-muted mb-0">${msg.text("jsonDuality.subtitle")}</p>
            </div>
            <div id="json-duality-page" data-document-url="/owners/${owner.id()}/json-duality"
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
                                    <li><code>@JsonView</code> maps the owner document.</li>
                                    <li><code>@JsonSubView</code> maps the nested pets and visits.</li>
                                    <li><code>@Relation</code> declares their nested collections.</li>
                                    <li>The Oracle repository reads and updates the generated view.</li>
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
                                <div id="document-status" class="mt-3" role="status" aria-live="polite" hidden></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <script>
                (() => {
                    const page = document.getElementById("json-duality-page");
                    const editor = document.getElementById("owner-document");
                    const status = document.getElementById("document-status");
                    const documentUrl = page.dataset.documentUrl;
                    const setStatus = (message, style = "info") => {
                        const hasMessage = Boolean(message);
                        status.hidden = !hasMessage;
                        status.className = hasMessage ? "mt-3 alert alert-" + style : "mt-3";
                        status.textContent = hasMessage ? message : "";
                    };
                    const loadDocument = async () => {
                        setStatus(page.dataset.loadingLabel, "secondary");
                        try {
                            const response = await fetch(documentUrl, { headers: { Accept: "application/json" } });
                            if (!response.ok) throw new Error(page.dataset.loadErrorLabel);
                            editor.value = JSON.stringify(await response.json(), null, 2);
                            setStatus("");
                        } catch (error) {
                            setStatus(error.message || page.dataset.loadErrorLabel, "danger");
                        }
                    };
                    document.getElementById("reload-document").addEventListener("click", loadDocument);
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
                            setStatus(page.dataset.saveLabel, "success");
                        } catch (error) {
                            setStatus(error.message || page.dataset.saveErrorLabel, "danger");
                        }
                    });
                    loadDocument();
                })();
            </script>
        @endif
    </main>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
