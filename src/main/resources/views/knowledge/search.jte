@param String query
@param java.util.List<io.micronaut.samples.petclinic.dto.PetCareSearchResult> results
@param int dimensions
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-brain me-3"></i>${msg.text("knowledge.title")}</h1>
            <p>${msg.text("knowledge.subtitle")}</p>
        </div>
    </div>

    <main id="knowledge-page" class="container flex-grow-1 py-4"
          data-results-title-label="${msg.text("knowledge.resultsTitle")}"
          data-matches-template="${msg.text("knowledge.matches", "__COUNT__")}"
          data-no-matches-label="${msg.text("knowledge.noMatches")}"
          data-distance-template="${msg.text("knowledge.distance", "__VALUE__")}"
          data-chunk-template="${msg.text("knowledge.chunk", "__VALUE__")}"
          data-similarity-template="${msg.text("knowledge.similarity", "__VALUE__")}"
          data-loading-label="${msg.text("knowledge.loading")}"
          data-error-label="${msg.text("knowledge.searchError")}">
        <div class="card mb-4 animate-fade-in-up">
            <div class="card-body">
                <form action="/knowledge" method="get" id="knowledge-search-form">
                    <label for="query" class="form-label">
                        <i class="fas fa-search me-2"></i>${msg.text("knowledge.queryLabel")}
                    </label>
                    <div class="input-group input-group-lg">
                        <input type="text" class="form-control" id="query" name="query"
                               value="${query != null ? query : ""}"
                               placeholder="${msg.text("knowledge.queryPlaceholder")}">
                        <button class="btn btn-primary" type="submit">
                            <i class="fas fa-search me-2"></i>${msg.text("knowledge.retrieveChunks")}
                        </button>
                    </div>
                </form>
                <div class="mt-3 text-muted small">
                    <i class="fas fa-database me-1"></i>
                    ${msg.text("knowledge.searchMetadata", dimensions)}
                </div>
            </div>
        </div>

        <div class="mb-3">
            <h2 class="h5 mb-1">${msg.text("knowledge.quickSearch.title")}</h2>
            <p class="text-muted small mb-0">${msg.text("knowledge.quickSearch.description")}</p>
        </div>
        <div class="row g-3 mb-4">
                <div class="col-md-4"><a class="card h-100 text-decoration-none" data-knowledge-query="What vaccinations does my puppy need?" href="/knowledge?query=What+vaccinations+does+my+puppy+need%3F">
                    <div class="card-body"><h5><i class="fas fa-syringe text-primary me-2"></i>${msg.text("knowledge.quickSearch.puppy.title")}</h5><p class="text-muted mb-0">${msg.text("knowledge.quickSearch.puppy.description")}</p></div>
                </a></div>
                <div class="col-md-4"><a class="card h-100 text-decoration-none" data-knowledge-query="When should a young dog receive its shots?" href="/knowledge?query=When+should+a+young+dog+receive+its+shots%3F">
                    <div class="card-body"><h5><i class="fas fa-calendar-check text-info me-2"></i>${msg.text("knowledge.quickSearch.dog.title")}</h5><p class="text-muted mb-0">${msg.text("knowledge.quickSearch.dog.description")}</p></div>
                </a></div>
                <div class="col-md-4"><a class="card h-100 text-decoration-none" data-knowledge-query="How should I clean my bird's cage?" href="/knowledge?query=How+should+I+clean+my+bird%27s+cage%3F">
                    <div class="card-body"><h5><i class="fas fa-dove text-warning me-2"></i>${msg.text("knowledge.quickSearch.bird.title")}</h5><p class="text-muted mb-0">${msg.text("knowledge.quickSearch.bird.description")}</p></div>
                </a></div>
                <div class="col-md-4"><a class="card h-100 text-decoration-none" data-knowledge-query="What temperature and humidity does a reptile habitat need?" href="/knowledge?query=What+temperature+and+humidity+does+a+reptile+habitat+need%3F">
                    <div class="card-body"><h5><i class="fas fa-temperature-half text-danger me-2"></i>${msg.text("knowledge.quickSearch.reptile.title")}</h5><p class="text-muted mb-0">${msg.text("knowledge.quickSearch.reptile.description")}</p></div>
                </a></div>
                <div class="col-md-4"><a class="card h-100 text-decoration-none" data-knowledge-query="How can I improve my cat's nutrition" href="/knowledge?query=How+can+I+improve+my+cat%27s+nutrition">
                    <div class="card-body"><h5><i class="fas fa-bowl-food text-success me-2"></i>${msg.text("knowledge.quickSearch.cat.title")}</h5><p class="text-muted mb-0">${msg.text("knowledge.quickSearch.cat.description")}</p></div>
                </a></div>
                <div class="col-md-4"><a class="card h-100 text-decoration-none" data-knowledge-query="What are pet poisoning warning signs" href="/knowledge?query=What+are+pet+poisoning+warning+signs">
                    <div class="card-body"><h5><i class="fas fa-triangle-exclamation text-danger me-2"></i>${msg.text("knowledge.quickSearch.emergency.title")}</h5><p class="text-muted mb-0">${msg.text("knowledge.quickSearch.emergency.description")}</p></div>
                </a></div>
        </div>

        <div id="knowledge-results" aria-live="polite">
        @if(query != null && !query.isBlank())
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h2 class="h4 mb-0">${msg.text("knowledge.resultsTitle")}</h2>
                <span class="badge bg-primary">${msg.text("knowledge.matches", results.size())}</span>
            </div>

            @if(results.isEmpty())
                <div class="alert alert-warning">
                    <i class="fas fa-circle-info me-2"></i>${msg.text("knowledge.noMatches")}
                </div>
            @else
                @for(io.micronaut.samples.petclinic.dto.PetCareSearchResult result : results)
                    <article class="card mb-3 animate-fade-in-up">
                        <div class="card-body">
                            <div class="d-flex flex-wrap justify-content-between gap-2 mb-2">
                                <div>
                                    <span class="badge bg-secondary me-2">${result.topic()}</span>
                                    <span class="badge bg-light text-dark">${result.species()}</span>
                                </div>
                                <span class="text-muted small">${msg.text("knowledge.distance", result.distance())}</span>
                            </div>
                            <h3 class="h5">${result.documentTitle()}</h3>
                            <div class="text-muted small mb-2">${result.documentSource()}</div>
                            <p class="mb-2">${result.content()}</p>
                            <div class="text-muted small">
                                ${msg.text("knowledge.chunk", result.chunkIndex() + 1)}
                                @if(result.similarity() != null)
                                    &middot; ${msg.text("knowledge.similarity", result.similarity())}
                                @endif
                            </div>
                        </div>
                    </article>
                @endfor
            @endif
        @endif
        </div>

        <div class="card mt-5 border-primary">
            <div class="card-body">
                <h3 class="h5"><i class="fas fa-code me-2"></i>${msg.text("knowledge.api.title")}</h3>
                <code>POST /knowledge/search</code>
                <pre class="mt-2 mb-0"><code>${msg.text("knowledge.api.requestExample")}</code></pre>
            </div>
        </div>
    </main>

    <script>
        (() => {
            const page = document.getElementById("knowledge-page");
            const form = document.getElementById("knowledge-search-form");
            const input = document.getElementById("query");
            const results = document.getElementById("knowledge-results");
            const submit = form.querySelector("button[type=submit]");
            const labels = page.dataset;

            const escapeHtml = (value) => String(value ?? "")
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll('"', "&quot;")
                .replaceAll("'", "&#039;");

            const formatLabel = (template, value, token = "__VALUE__") =>
                escapeHtml(template).replace(token, escapeHtml(value));

            const renderResult = (result) => {
                const similarity = result.similarity == null
                    ? ""
                    : " &middot; " + formatLabel(labels.similarityTemplate, result.similarity);
                return "<article class=\"card mb-3 animate-fade-in-up\">"
                    + "<div class=\"card-body\">"
                    + "<div class=\"d-flex flex-wrap justify-content-between gap-2 mb-2\">"
                    + "<div><span class=\"badge bg-secondary me-2\">"
                    + escapeHtml(result.topic)
                    + "</span><span class=\"badge bg-light text-dark\">"
                    + escapeHtml(result.species)
                    + "</span></div><span class=\"text-muted small\">"
                    + formatLabel(labels.distanceTemplate, result.distance)
                    + "</span></div>"
                    + "<h3 class=\"h5\">"
                    + escapeHtml(result.documentTitle)
                    + "</h3><div class=\"text-muted small mb-2\">"
                    + escapeHtml(result.documentSource)
                    + "</div><p class=\"mb-2\">"
                    + escapeHtml(result.content)
                    + "</p><div class=\"text-muted small\">"
                    + formatLabel(labels.chunkTemplate, (result.chunkIndex ?? 0) + 1)
                    + similarity
                    + "</div></div></article>";
            };

            const renderResults = (items) => {
                const heading = "<div class=\"d-flex justify-content-between align-items-center mb-3\">"
                    + "<h2 class=\"h4 mb-0\">"
                    + escapeHtml(labels.resultsTitleLabel)
                    + "</h2><span class=\"badge bg-primary\">"
                    + formatLabel(labels.matchesTemplate, items.length, "__COUNT__")
                    + "</span></div>";
                const content = items.length > 0
                    ? items.map(renderResult).join("")
                    : "<div class=\"alert alert-warning\"><i class=\"fas fa-circle-info me-2\"></i>"
                        + escapeHtml(labels.noMatchesLabel)
                        + "</div>";
                results.innerHTML = heading + content;
            };

            const search = async (query) => {
                const trimmedQuery = query.trim();
                input.value = query;
                if (!trimmedQuery) {
                    history.replaceState(null, "", window.location.pathname);
                    results.innerHTML = "";
                    return;
                }

                history.replaceState(null, "", window.location.pathname + "?query="
                    + encodeURIComponent(query));
                submit.disabled = true;
                results.setAttribute("aria-busy", "true");
                results.innerHTML = "<div class=\"text-center py-4\" role=\"status\">"
                    + "<div class=\"spinner-border text-primary\" aria-hidden=\"true\"></div>"
                    + "<span class=\"visually-hidden\">"
                    + escapeHtml(labels.loadingLabel)
                    + "</span></div>";

                try {
                    const response = await fetch("/knowledge/search", {
                        method: "POST",
                        headers: {
                            Accept: "application/json",
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({query: query})
                    });
                    if (!response.ok) {
                        throw new Error("Search request failed");
                    }
                    renderResults(await response.json());
                } catch (exception) {
                    results.innerHTML = "<div class=\"alert alert-danger\">"
                        + escapeHtml(labels.errorLabel)
                        + "</div>";
                } finally {
                    submit.disabled = false;
                    results.removeAttribute("aria-busy");
                }
            };

            form.addEventListener("submit", (event) => {
                event.preventDefault();
                search(input.value);
            });

            document.querySelectorAll("[data-knowledge-query]").forEach((link) => {
                link.addEventListener("click", (event) => {
                    event.preventDefault();
                    search(link.dataset.knowledgeQuery);
                });
            });
        })();
    </script>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
