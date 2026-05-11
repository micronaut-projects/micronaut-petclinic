@import io.micronaut.samples.petclinic.dto.VisitForm
@import io.micronaut.samples.petclinic.model.Owner
@import io.micronaut.samples.petclinic.model.Pet
@import io.micronaut.samples.petclinic.model.Visit
@import java.util.Locale

@param VisitForm visit
@param Pet pet
@param Owner owner
@param java.util.Map<String, String> validationErrors
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request
@param java.util.Locale locale

@template.layout(content = @`
    <div class="page-header">
        <div class="container">
            <h1><i class="fas fa-calendar-plus me-3"></i>${msg.text("visit.new")}</h1>
            @if(pet != null)
                <p>
                    <i class="fas fa-paw me-2"></i>${msg.text("pet.name")}: ${pet.name()}
                </p>
            @endif
        </div>
    </div>

    <main class="container flex-grow-1 py-4">
        <div class="row">
            <div class="col-12 mb-4">
                <a href="/owners/${owner.id()}" class="btn btn-secondary">
                    <i class="fas fa-arrow-left me-2"></i>${msg.text("pet.backToOwner")}
                </a>
            </div>

            <div class="col-lg-4 mb-4">
                @if(pet != null)
                    <div class="detail-card animate-fade-in-up">
                        <div class="card-header">
                            <h5 class="mb-0"><i class="fas fa-paw me-2"></i>${msg.text("pet.information")}</h5>
                        </div>
                        <div class="detail-body">
                            <div class="text-center mb-4">
                                @{
                                    String typeKey = (pet.getType() != null && pet.getType().name() != null)
                                        ? pet.getType().name().trim().toLowerCase(Locale.ROOT)
                                        : "";
                                    String iconClass = switch (typeKey) {
                                        case "cat" -> "fa-cat";
                                        case "bird" -> "fa-dove";
                                        case "hamster" -> "fa-hippo";
                                        case "snake" -> "fa-worm";
                                        default -> "fa-dog";
                                    };
                                }
                                <div class="pet-icon mx-auto mb-3 ${typeKey.isBlank() ? "dog" : typeKey}" style="width: 80px; height: 80px; font-size: 2rem;">
                                    <i class="fas ${iconClass}"></i>
                                </div>
                                <h5>${pet.name()}</h5>
                            </div>
                            <div class="info-row">
                                <div class="info-label">${msg.text("pet.type")}</div>
                                <div class="info-value">${pet.getType() != null ? pet.getType().name() : msg.text("visit.unknown")}</div>
                            </div>
                            <div class="info-row">
                                <div class="info-label">${msg.text("pet.birthDate")}</div>
                                <div class="info-value">${pet.getBirthDate() != null ? pet.getBirthDate().toString() : ""}</div>
                            </div>
                            <div class="info-row">
                                <div class="info-label">${msg.text("pet.owner")}</div>
                                <div class="info-value">${owner.getFirstName()} ${owner.getLastName()}</div>
                            </div>
                        </div>
                    </div>

                    @if(pet.getVisits() != null && !pet.getVisits().isEmpty())
                        <div class="card mt-4 animate-fade-in-up stagger-1">
                            <div class="card-header">
                                <h5 class="mb-0"><i class="fas fa-history me-2"></i>${msg.text("pet.previousVisits")}</h5>
                            </div>
                            <div class="card-body">
                                <div class="visit-timeline">
                                    @for(Visit previousVisit : pet.getVisits())
                                        <div class="visit-item">
                                            <div class="visit-date">${previousVisit.date() != null ? previousVisit.date().toString() : ""}</div>
                                            <div class="visit-description">${previousVisit.description()}</div>
                                        </div>
                                    @endfor
                                </div>
                            </div>
                        </div>
                    @endif
                @endif
            </div>

            <div class="col-lg-8">
                <div class="form-container animate-fade-in-up stagger-2">
                    <div class="text-center mb-4">
                        <div class="feature-icon gradient-3 mx-auto mb-3">
                            <i class="fas fa-stethoscope"></i>
                        </div>
                        <h4>${msg.text("visit.scheduleNew")}</h4>
                        <p class="text-muted">${msg.text("visit.recordFor")} ${pet.name()}</p>
                    </div>

                    @if(validationErrors != null && !validationErrors.isEmpty())
                        <div class="alert alert-danger" role="alert">
                            ${msg.text("validation.fixErrors")}
                        </div>
                    @endif
 
                    <form action="/owners/${owner.id()}/pets/${pet.id()}/visits/new" method="post" class="needs-validation" novalidate>
                        <div class="mb-4">
                            <label for="date" class="form-label">
                                <i class="fas fa-calendar me-2 text-primary"></i>${msg.text("visit.visitDate")}
                            </label>
                            <input type="date" class="form-control ${validationErrors != null && validationErrors.containsKey("date") ? "is-invalid" : ""}" id="date" name="date"
                                   value="${visit != null && visit.date() != null ? visit.date().toString() : ""}" required>
                            @if(validationErrors != null && validationErrors.containsKey("date"))
                                <div class="invalid-feedback">${validationErrors.get("date")}</div>
                            @else
                                <div class="invalid-feedback">${msg.text("validation.visitDate.required")}</div>
                            @endif
                        </div>
 
                        <div class="mb-4">
                            <label for="description" class="form-label">
                                <i class="fas fa-notes-medical me-2 text-primary"></i>${msg.text("visit.description")}
                            </label>
                            <textarea class="form-control ${validationErrors != null && validationErrors.containsKey("description") ? "is-invalid" : ""}" id="description" name="description" rows="4" required>${visit != null && visit.description() != null ? visit.description() : ""}</textarea>
                            @if(validationErrors != null && validationErrors.containsKey("description"))
                                <div class="invalid-feedback">${validationErrors.get("description")}</div>
                            @else
                                <div class="invalid-feedback">${msg.text("validation.description.required")}</div>
                            @endif
                        </div>

                        <hr class="my-4">

                        <div class="d-flex gap-3">
                            <button type="submit" class="btn btn-primary btn-lg flex-grow-1">
                                <i class="fas fa-plus me-2"></i>${msg.text("button.addVisit")}
                            </button>
                            <a href="/owners/${owner.id()}" class="btn btn-secondary btn-lg">
                                <i class="fas fa-times me-2"></i>${msg.text("button.cancel")}
                            </a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </main>
`, msg = msg, request = request, locale = locale)
