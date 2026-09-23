@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale
@param java.util.Set<String> activeNames
@param java.util.List<io.micronaut.samples.petclinic.model.Appointment> appointments
@param int reservationSeconds

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-bolt me-3"></i>${msg.text("transactionPriority.title")}</h1>
            <p>${msg.text("transactionPriority.intro")}</p>
        </div>
    </div>
    <main id="transaction-priority-page" class="container flex-grow-1 py-4" data-reservation-seconds="${reservationSeconds}">
        <div hidden>
            @for(var key : java.util.List.of("error", "noAvailable", "confirmed", "emergencyPending", "regularWon", "taken", "working", "timedOut", "countdown", "completing", "finished", "outcome.NOT_STARTED", "outcome.RUNNING"))
                <span data-key="${key}">${msg.text("transactionPriority." + key)}</span>
            @endfor
            @for(var outcome : io.micronaut.samples.petclinic.dto.OracleBookingResult.Outcome.values())
                <span data-key="outcome.${outcome.name()}">${msg.text("transactionPriority.outcome." + outcome.name())}</span>
            @endfor
        </div>
        <section class="card mb-4">
            <div class="card-body">
                <h2 class="h4">${msg.text("transactionPriority.controls")}</h2>
                <p class="text-muted">${msg.text("transactionPriority.instructions")}</p>
                <div class="mb-3">
                    <label for="priority-appointment-select" class="form-label">${msg.text("transactionPriority.availableAppointments")}</label>
                    <select id="priority-appointment-select" class="form-select">
                        @for(var appointment : appointments)
                            <option value="${appointment.id()}">${appointment.displayOrder()}. ${appointment.label()}</option>
                        @endfor
                        @if(appointments.isEmpty())
                            <option value="">${msg.text("transactionPriority.noAvailable")}</option>
                        @endif
                    </select>
                </div>
                <div class="d-flex flex-wrap gap-2 mb-3">
                    <button id="priority-start-low" type="button" class="btn btn-warning">
                        <i class="fas fa-user"></i> ${msg.text("transactionPriority.startLow")}
                    </button>
                    <button id="priority-start-high" type="button" class="btn btn-success" disabled>
                        <i class="fas fa-triangle-exclamation"></i> ${msg.text("transactionPriority.startHigh")}
                    </button>
                    <button id="priority-reset" type="button" class="btn btn-outline-secondary">
                        <i class="fas fa-rotate-left"></i> ${msg.text("transactionPriority.reset")}
                    </button>
                </div>
                <p id="priority-error" class="alert alert-danger mt-3 mb-0 d-none" role="alert"></p>
            </div>
        </section>
        <section id="priority-state" class="d-none" aria-live="polite">
            <div class="row g-3 mb-4">
                <div class="col-md-6">
                    <div class="card h-100 priority-booking low">
                        <div class="card-body">
                            <h2 class="h5">LOW · ${msg.text("transactionPriority.regular")}</h2>
                            <p id="priority-low" class="mb-0"></p>
                            <p id="priority-countdown" class="text-muted mt-3 mb-1"></p>
                            <div id="priority-countdown-value" class="fs-2 fw-bold font-monospace" role="timer" aria-live="off"></div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card h-100 priority-booking high">
                        <div class="card-body">
                            <h2 class="h5">HIGH · ${msg.text("transactionPriority.emergency")}</h2>
                            <p id="priority-high" class="mb-0"></p>
                        </div>
                    </div>
                </div>
            </div>
            <div class="card mb-4">
                <div class="card-body">
                    <h2 class="h5">${msg.text("transactionPriority.result")}</h2>
                    <p id="priority-summary"></p>
                    <dl class="row mb-0">
                        <dt class="col-sm-4">${msg.text("transactionPriority.appointment")}</dt>
                        <dd id="priority-appointment" class="col-sm-8"></dd>
                        <dt class="col-sm-4">${msg.text("transactionPriority.databaseStatus")}</dt>
                        <dd id="priority-database-status" class="col-sm-8"></dd>
                        <dt class="col-sm-4">${msg.text("transactionPriority.suggestion")}</dt>
                        <dd id="priority-suggestion" class="col-sm-8"></dd>
                    </dl>
                </div>
            </div>
        </section>
        <details class="card">
            <summary class="card-body fw-bold">${msg.text("transactionPriority.how")}</summary>
            <div class="card-body pt-0">
                <p>${msg.text("transactionPriority.explanation")}</p>
                <p class="text-muted mb-0">${msg.text("transactionPriority.limitations")}</p>
            </div>
        </details>
    </main>
    <script type="module" src="/resources/js/transaction-priority.js"></script>
`, msg = msg, request = request, locale = locale, activeNames = activeNames)
