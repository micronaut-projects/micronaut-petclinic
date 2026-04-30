@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request

@template.layout(content = @`
    <div class="container-fluid px-0">
        <section class="hero-section text-white" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 60vh; display: flex; align-items: center;">
            <div class="container">
                <div class="row align-items-center">
                    <div class="col-lg-8">
                        <h1 class="animate-fade-in-up">${msg.text("welcome.title")} <i class="fas fa-paw"></i></h1>
                        <p class="animate-fade-in-up stagger-1">${msg.text("welcome.subtitle")}</p>
                        <div class="d-flex gap-3 mt-4 animate-fade-in-up stagger-2">
                            <a href="/owners/find" class="btn btn-lg btn-light">
                                <i class="fas fa-search"></i> ${msg.text("welcome.findOwners")}
                            </a>
                            <a href="/vets" class="btn btn-lg btn-outline-light">
                                <i class="fas fa-user-md"></i> ${msg.text("welcome.viewVets")}
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </div>
`, msg = msg, request = request)
