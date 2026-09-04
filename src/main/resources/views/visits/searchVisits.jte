@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-calendar-check me-3"></i>${msg.text("visit.search.title")}</h1>
            <p>${msg.text("visit.search.subtitle")}</p>
        </div>
    </div>

    <main id="visit-search-page" class="container flex-grow-1 py-4"
          data-unknown-label="${msg.text("visit.unknown")}"
          data-minutes-label="${msg.text("visit.minutes")}"
          data-months-label="${msg.text("visit.months")}"
          data-matches-label="${msg.text("visit.search.matches")}"
          data-error-label="${msg.text("visit.search.error")}">
        <div class="search-box mb-4 animate-fade-in-up">
            <form action="/visits/search" method="get" id="visit-search-form">
                <div class="row g-3 mb-3">
                    <div class="col-md-3">
                        <label for="fromDate" class="form-label">${msg.text("visit.search.fromDate")}</label>
                        <input type="date" class="form-control" id="fromDate" name="fromDate">
                    </div>
                    <div class="col-md-3">
                        <label for="toDate" class="form-label">${msg.text("visit.search.toDate")}</label>
                        <input type="date" class="form-control" id="toDate" name="toDate">
                    </div>
                    <div class="col-md-3">
                        <label for="maxDurationMinutes"
                               class="form-label">${msg.text("visit.search.durationLessThan")}</label>
                        <div class="d-flex align-items-center gap-2">
                            <input type="range" class="form-range" id="maxDurationMinutes" name="maxDurationMinutes"
                                   min="0" max="240" step="15" value="120">
                            <output id="durationValue" data-any-label="${msg.text("visit.search.any")}"
                                    class="text-nowrap">
                            </output>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <label for="maxFollowUpMonths"
                               class="form-label">${msg.text("visit.search.followUpLessThan")}</label>
                        <div class="d-flex align-items-center gap-2">
                            <input type="range" class="form-range" id="maxFollowUpMonths" name="maxFollowUpMonths"
                                   min="0" max="36" step="1" value="12">
                            <output id="followUpValue" data-any-label="${msg.text("visit.search.any")}"
                                    class="text-nowrap">
                            </output>
                        </div>
                    </div>
                </div>

                <div class="row g-3 align-items-end">
                    <div class="col-md-6 offset-md-6 d-flex gap-2">
                        <button type="submit" class="btn btn-primary flex-grow-1">
                            <i class="fas fa-search me-2"></i>${msg.text("button.search")}
                        </button>
                        <a href="/visits" class="btn btn-outline-secondary">
                            <i class="fas fa-rotate-left me-2"></i>${msg.text("button.reset")}
                        </a>
                    </div>
                </div>
            </form>
        </div>

        <div class="d-flex justify-content-between align-items-center mb-3">
            <h2 class="h4 mb-0">${msg.text("visit.search.results")}</h2>
            <span class="badge bg-primary rounded-pill" id="visit-results-count">0 ${msg.text("visit.search.matches")}</span>
        </div>

        <div id="visit-loading" class="text-center py-5 d-none" aria-live="polite">
            <div class="spinner-border text-primary" role="status"></div>
        </div>
        <div id="visit-error" class="alert alert-danger d-none" role="alert"></div>
        <div id="visit-empty" class="empty-state text-center py-5">
            <i class="fas fa-calendar-xmark fa-3x text-muted mb-3"></i>
            <h3 class="h5">${msg.text("visit.search.noResults")}</h3>
            <p class="text-muted">${msg.text("visit.search.tryDifferent")}</p>
        </div>
        <div id="visit-results" class="row g-3"></div>
    </main>

    <script>
        (() => {
            const page = document.getElementById("visit-search-page");
            const form = document.getElementById("visit-search-form");
            const results = document.getElementById("visit-results");
            const empty = document.getElementById("visit-empty");
            const error = document.getElementById("visit-error");
            const loading = document.getElementById("visit-loading");
            const count = document.getElementById("visit-results-count");
            const labels = page.dataset;

            const bindRange = (inputId, outputId) => {
                const input = document.getElementById(inputId);
                const output = document.getElementById(outputId);
                const update = () => {
                    output.textContent = input.value;
                };
                input.addEventListener("input", update);
                update();
            };

            const escapeHtml = (value) => String(value)
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#039;");

            const formatValue = (value, unit) => {
                return value === null || value === undefined
                    ? labels.unknownLabel
                    : escapeHtml(value) + " " + escapeHtml(unit);
            };

            const renderVisit = (visit) => {
                const patient = visit.petName
                    ? "<i class=\"fas fa-paw me-1\"></i>" + escapeHtml(visit.petName)
                        + (visit.ownerName ? "<span class=\"mx-1\">·</span>"
                            + escapeHtml(visit.ownerName) : "")
                    : escapeHtml(labels.unknownLabel);

                return "<div class=\"col-12 col-lg-6\">"
                    + "<article class=\"card h-100 shadow-sm\"><div class=\"card-body\">"
                    + "<div class=\"d-flex justify-content-between gap-3 mb-2\">"
                    + "<h3 class=\"h5 mb-0\">" + escapeHtml(visit.description || labels.unknownLabel) + "</h3>"
                    + "<span class=\"text-muted text-nowrap\">" + escapeHtml(visit.date || "") + "</span>"
                    + "</div><p class=\"mb-3 text-muted\">" + patient + "</p>"
                    + "<div class=\"visit-meta\">"
                    + "<span><i class=\"fas fa-clock me-1\"></i>"
                    + formatValue(visit.durationInMinutes, labels.minutesLabel) + "</span>"
                    + "<span><i class=\"fas fa-calendar-check me-1\"></i>"
                    + formatValue(visit.periodInMonths, labels.monthsLabel) + "</span>"
                    + "</div></div></article></div>";
            };

            const renderResults = (visits) => {
                count.textContent = visits.length + " " + labels.matchesLabel;
                results.innerHTML = visits.map(renderVisit).join("");
                empty.classList.toggle("d-none", visits.length > 0);
            };

            const searchVisits = async (event) => {
                if (event) {
                    event.preventDefault();
                }

                const params = new URLSearchParams();
                for (const [name, value] of new FormData(form).entries()) {
                    if (value) {
                        params.set(name, value);
                    }
                }

                if (!params.has("fromDate")) {
                    const lastYear = new Date();
                    lastYear.setFullYear(lastYear.getFullYear() - 1);
                    const lastYearFormatted = lastYear.toISOString().split('T')[0]
                    params.set("fromDate", lastYearFormatted);

                    document.getElementById("fromDate").value = lastYearFormatted
                }
                if (!params.has("toDate")) {
                    const date = new Date().toISOString().split('T')[0]
                    params.set("toDate", date);
                    document.getElementById("toDate").value = date
                }

                loading.classList.remove("d-none");
                error.classList.add("d-none");
                empty.classList.add("d-none");
                results.innerHTML = "";

                try {
                    const query = params.toString();
                    const response = await fetch(form.action + (query ? "?" + query : ""), {
                        headers: {Accept: "application/json"}
                    });
                    if (!response.ok) {
                        throw new Error("Search request failed");
                    }
                    renderResults(await response.json());
                } catch (exception) {
                    error.textContent = labels.errorLabel;
                    error.classList.remove("d-none");
                } finally {
                    loading.classList.add("d-none");
                }
            };

            bindRange("maxDurationMinutes", "durationValue");
            bindRange("maxFollowUpMonths", "followUpValue");
            form.addEventListener("submit", searchVisits);
            searchVisits();
        })();
    </script>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
