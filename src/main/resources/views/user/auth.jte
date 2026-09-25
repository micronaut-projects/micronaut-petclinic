@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-right-to-bracket me-3"></i>${msg.text("auth.signIn.title")}</h1>
            <p>${msg.text("auth.signIn.subtitle", msg.text("app.title"))}</p>
        </div>
    </div>
    <main class="container flex-grow-1 py-4">
        <div class="row justify-content-center">
            <div class="col-lg-10 col-xl-9">
                <div class="form-container animate-fade-in-up p-0 overflow-hidden">
                    <div class="row g-0 align-items-stretch">
                        <div class="col-lg-5 d-none d-lg-flex detail-header">
                            <div class="d-flex flex-column justify-content-between w-100">
                                <div>
                                    <div class="detail-avatar mb-4">
                                        <i class="fas fa-paw"></i>
                                    </div>
                                    <h2 class="text-white mb-3">${msg.text("auth.signIn.panelTitle")}</h2>
                                    <p class="mb-0" style="color: rgba(255,255,255,0.85);">
                                        ${msg.text("auth.signIn.panelDescription")}
                                    </p>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-7">
                            <div class="p-4 p-md-5">
                                <div class="text-center text-lg-start mb-4">
                                    <div class="feature-icon gradient-1 d-inline-flex d-lg-none mb-3">
                                        <i class="fas fa-paw"></i>
                                    </div>
                                    <h2 class="mb-2">${msg.text("auth.signIn.heading")}</h2>
                                    <p class="text-muted mb-0">${msg.text("auth.signIn.description")}</p>
                                </div>
                                @if(request != null && request.getPath() != null && request.getPath().contains("authFailed"))
                                    <div class="alert alert-danger mb-4">
                                        <i class="fas fa-circle-exclamation"></i>
                                        <span>${msg.text("auth.signIn.invalid")}</span>
                                    </div>
                                @endif
                                <form action="/login" method="post" class="needs-validation" novalidate>
                                    <div class="mb-3">
                                        <label for="username" class="form-label">
                                            <i class="fas fa-user me-2"></i>${msg.text("auth.username")}
                                        </label>
                                        <input type="text"
                                               class="form-control form-control-lg"
                                               id="username"
                                               name="username"
                                               autocomplete="username"
                                               placeholder="${msg.text("auth.username.placeholder")}"
                                               required>
                                        <div class="invalid-feedback">${msg.text("auth.username.required")}</div>
                                    </div>
                                    <div class="mb-4">
                                        <label for="password" class="form-label">
                                            <i class="fas fa-lock me-2"></i>${msg.text("auth.password")}
                                        </label>
                                        <input type="password"
                                               class="form-control form-control-lg"
                                               id="password"
                                               name="password"
                                               autocomplete="current-password"
                                               placeholder="${msg.text("auth.password.placeholder")}"
                                               required>
                                        <div class="invalid-feedback">${msg.text("auth.password.required")}</div>
                                    </div>
                                    <div class="d-flex align-items-center justify-content-between gap-3 flex-wrap mb-4">
                                        <div class="form-check">
                                            <input class="form-check-input" type="checkbox" value="true" id="rememberMe"
                                                   name="rememberMe">
                                            <label class="form-check-label text-muted" for="rememberMe">${msg.text("auth.rememberMe")}</label>
                                        </div>
                                        <a href="/user/signUp" class="btn btn-outline-primary">
                                            <i class="fas fa-user-plus me-2"></i>${msg.text("auth.createAccount")}
                                        </a>
                                    </div>
                                    <button class="btn btn-primary btn-lg w-100" type="submit">
                                        <i class="fas fa-right-to-bracket me-2"></i>${msg.text("auth.signIn.action")}
                                    </button>
                                </form>
                                <div class="text-center mt-4">
                                    <a href="/" class="text-decoration-none text-muted">
                                        <i class="fas fa-arrow-left me-2"></i>${msg.text("auth.backHome")}
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
