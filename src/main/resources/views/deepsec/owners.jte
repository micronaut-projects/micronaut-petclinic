@import io.micronaut.samples.petclinic.controller.DeepDataSecurityController.OwnerData
@import java.util.List

@param String user
@param String subject
@param List<String> roles
@param List<OwnerData> owners
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale

@template.layout(content = @`
    <div class="page-header deepsec-page-header">
        <div class="container">
            <div class="d-flex align-items-center gap-3 mb-2">
                <span class="deepsec-header-icon"><i class="fas fa-shield-halved"></i></span>
                <span class="deepsec-eyebrow">Oracle Deep Data Security</span>
            </div>
            <h1>Authorized Pet Owners</h1>
            <p>This page displays the rows returned by Oracle for the signed-in Entra identity.</p>
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <section class="deepsec-identity-card mb-4 animate-fade-in-up">
            <div class="row align-items-center g-4">
                <div class="col-lg-7">
                    <div class="d-flex align-items-start gap-3">
                        <div class="deepsec-status-icon">
                            <i class="fas fa-check"></i>
                        </div>
                        <div>
                            <div class="d-flex align-items-center gap-2 flex-wrap mb-2">
                                <h2 class="h4 mb-0">Microsoft Entra ID verified</h2>
                                <span class="badge rounded-pill text-bg-success">Connected</span>
                            </div>
                            <p class="mb-1 text-muted">Signed in as</p>
                            <div class="deepsec-user">${user}</div>
                            <div class="deepsec-subject">Subject: ${subject}</div>
                            <a href="/oauth/logout" class="btn btn-sm btn-outline-danger mt-3">
                                <i class="fas fa-right-from-bracket me-1"></i>Log out
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <section class="mb-5">
            <div class="mb-3">
                <h2 class="h3 mb-1">Role escalation demo</h2>
            </div>
            <div class="row g-3">
                <div class="col-md-6">
                    <div class="deepsec-role-card h-100">
                        <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
                            <div>
                                <span class="badge rounded-pill text-bg-primary mb-2">EMPLOYEE</span>
                                <h3 class="h5 mb-1">Restricted owner scope</h3>
                            </div>
                            <i class="fas fa-user-lock deepsec-role-icon text-primary"></i>
                        </div>
                        <p class="small text-muted mb-0">Oracle matches the end-user email and returns only that user's owner row and related pets.</p>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="deepsec-role-card deepsec-role-card-elevated h-100">
                        <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
                            <div>
                                <span class="badge rounded-pill text-bg-success mb-2">STAFF</span>
                                <h3 class="h5 mb-1">Expanded clinic scope</h3>
                            </div>
                            <i class="fas fa-users-gear deepsec-role-icon text-success"></i>
                        </div>
                        <p class="small text-muted mb-0">Oracle returns all owner rows for staff. Staff can use the support action below to demonstrate method-scoped elevation.</p>
                    </div>
                </div>
            </div>
        </section>

        <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-4">
            <div>
                <h2 class="h3 mb-1">Pet owners visible to this user</h2>
            </div>
            <a href="/deepsec/owners.json" class="btn btn-outline-primary" target="_blank" rel="noopener noreferrer">
                <i class="fas fa-code me-2"></i>View JSON response
            </a>
        </div>

        @if(owners == null || owners.isEmpty())
            <div class="empty-state">
                <div class="empty-state-icon">
                    <i class="fas fa-shield-halved"></i>
                </div>
                <h3>No authorized owners</h3>
                <p>No owner rows were returned for this Entra identity.</p>
            </div>
        @else
            <div class="row g-4">
                @for(OwnerData owner : owners)
                    <div class="col-md-6 col-lg-4">
                        <div class="owner-card deepsec-owner-card animate-fade-in-up">
                            <div class="d-flex align-items-start mb-3">
                                <div class="owner-avatar me-3">
                                    <span>
                                        ${owner.firstName() != null && !owner.firstName().isEmpty() ? owner.firstName().substring(0, 1) : "?"}
                                        ${owner.lastName() != null && !owner.lastName().isEmpty() ? owner.lastName().substring(0, 1) : "?"}
                                    </span>
                                </div>
                                <div class="flex-grow-1">
                                    <div class="d-flex justify-content-between align-items-start gap-2">
                                        <h5 class="mb-1">${owner.firstName()} ${owner.lastName()}</h5>
                                        <span class="deepsec-row-badge"><i class="fas fa-lock-open"></i></span>
                                    </div>
                                    <p class="text-muted mb-0 small">
                                        <i class="fas fa-map-marker-alt me-1"></i>${owner.city()}
                                    </p>
                                </div>
                            </div>

                            <div class="mb-3">
                                <p class="mb-1 small text-muted">
                                    <i class="fas fa-home me-2"></i>${owner.address()}
                                </p>
                                @if(owner.telephone() != null)
                                    <p id="telephone-${owner.id()}" class="mb-0 small text-muted">
                                        <i class="fas fa-phone me-2"></i>${owner.telephone()}
                                    </p>
                                @else
                                    <p id="telephone-${owner.id()}" class="mb-0 small text-muted">
                                        <i class="fas fa-eye-slash me-2"></i>Telephone protected by Oracle
                                    </p>
                                @endif
                            </div>

                            @if(roles != null && roles.contains("STAFF"))
                                <div class="deepsec-support-control">
                                    <div id="support-status-${owner.id()}" class="small text-muted mb-2">
                                        <i class="fas fa-lock me-1"></i>Normal query result
                                    </div>
                                    <button type="button" class="btn btn-sm btn-outline-secondary w-100"
                                            onclick="revealSupportContact(${owner.id()}, this)">
                                        <i class="fas fa-key me-2"></i>RunAs support demo
                                    </button>
                                </div>
                            @endif

                            @if(owner.pets() != null && !owner.pets().isEmpty())
                                <div class="mb-2">
                                    <div class="d-flex flex-wrap">
                                        @for(String pet : owner.pets())
                                            <span class="pet-badge">
                                                <i class="fas fa-paw"></i>${pet}
                                            </span>
                                        @endfor
                                    </div>
                                </div>
                            @else
                                <div class="mb-2">
                                    <span class="text-muted small"><i class="fas fa-info-circle me-1"></i>No pets returned</span>
                                </div>
                            @endif
                        </div>
                    </div>
                @endfor
            </div>
        @endif

        <script>
            async function revealSupportContact(ownerId, button) {
                const status = document.getElementById("support-status-" + ownerId);
                const telephone = document.getElementById("telephone-" + ownerId);
                button.disabled = true;
                button.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Applying support role...';

                try {
                    const response = await fetch("/deepsec/owners/" + ownerId + "/support-contact", {
                        credentials: "same-origin",
                        headers: {"Accept": "application/json"}
                    });
                    if (!response.ok) {
                        throw new Error("Support lookup failed with status " + response.status);
                    }

                    const owner = await response.json();
                    telephone.replaceChildren();
                    const icon = document.createElement("i");
                    icon.className = "fas fa-phone me-2";
                    telephone.append(icon, owner.telephone || "No telephone available");
                    status.className = "small text-success mb-2";
                    status.innerHTML = '<i class="fas fa-shield-halved me-1"></i>Support role applied for this method only';
                    button.className = "btn btn-sm btn-success w-100";
                    button.innerHTML = '<i class="fas fa-check me-2"></i>Contact returned by Oracle';
                } catch (error) {
                    status.className = "small text-danger mb-2";
                    status.textContent = "Support lookup failed. Check the application logs.";
                    button.disabled = false;
                    button.innerHTML = '<i class="fas fa-key me-2"></i>Retry RunAs demo';
                }
            }
        </script>

    </main>
`, msg = msg, request = request, locale = locale)
