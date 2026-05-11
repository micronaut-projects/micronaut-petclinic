@param String path
@param String message
@param String exception
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1>${msg.text("error.title")}</h1>
            <p>${msg.text("error.subtitle")}</p>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="card">
            <div class="card-body">
                <h5 class="mb-3">${msg.text("error.details")}</h5>
                <div class="row g-3">
                    <div class="col-md-4">
                        <div class="text-muted small">${msg.text("error.requestedPath")}</div>
                        <div><code>${path}</code></div>
                    </div>
                    <div class="col-md-4">
                        <div class="text-muted small">${msg.text("error.exception")}</div>
                        <div><code>${exception}</code></div>
                    </div>
                    <div class="col-md-12">
                        <div class="text-muted small">${msg.text("error.message")}</div>
                        <div><code>${message != null ? message : ""}</code></div>
                    </div>
                </div>

                <div class="mt-4 d-flex gap-2 flex-wrap">
                    <a class="btn btn-primary" href="/">${msg.text("button.home")}</a>
                    <a class="btn btn-outline-secondary" href="/owners/find">${msg.text("owner.findOwners")}</a>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request, locale = locale)
