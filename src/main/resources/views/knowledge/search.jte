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

    <main class="container flex-grow-1 py-4">
        <div class="card mb-4 animate-fade-in-up">
            <div class="card-body">
                <form action="/knowledge" method="get">
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

        @if(query == null || query.isBlank())
            <div class="row g-3">
                <div class="col-md-4"><a class="card h-100 text-decoration-none" href="/knowledge?query=What+vaccinations+does+my+puppy+need">
                    <div class="card-body"><h5><i class="fas fa-syringe text-primary me-2"></i>${msg.text("knowledge.quickSearch.puppy.title")}</h5><p class="text-muted mb-0">${msg.text("knowledge.quickSearch.puppy.description")}</p></div>
                </a></div>
                <div class="col-md-4"><a class="card h-100 text-decoration-none" href="/knowledge?query=How+can+I+improve+my+cat%27s+nutrition">
                    <div class="card-body"><h5><i class="fas fa-bowl-food text-success me-2"></i>${msg.text("knowledge.quickSearch.cat.title")}</h5><p class="text-muted mb-0">${msg.text("knowledge.quickSearch.cat.description")}</p></div>
                </a></div>
                <div class="col-md-4"><a class="card h-100 text-decoration-none" href="/knowledge?query=What+are+pet+poisoning+warning+signs">
                    <div class="card-body"><h5><i class="fas fa-triangle-exclamation text-danger me-2"></i>${msg.text("knowledge.quickSearch.emergency.title")}</h5><p class="text-muted mb-0">${msg.text("knowledge.quickSearch.emergency.description")}</p></div>
                </a></div>
            </div>
        @else
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

        <div class="card mt-5 border-primary">
            <div class="card-body">
                <h3 class="h5"><i class="fas fa-code me-2"></i>${msg.text("knowledge.api.title")}</h3>
                <code>POST /knowledge/search</code>
                <pre class="mt-2 mb-0"><code>${msg.text("knowledge.api.requestExample")}</code></pre>
            </div>
        </div>
    </main>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
