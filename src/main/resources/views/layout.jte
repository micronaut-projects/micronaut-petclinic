@param gg.jte.Content content
@param io.micronaut.samples.petclinic.system.I18nViewModelProcessor.Msg msg
@param io.micronaut.http.HttpRequest<?> request

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${msg.text("app.title")} - ${msg.text("app.subtitle")}</title>

    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link href="/resources/css/petclinic.css" rel="stylesheet">
</head>
<body>
<nav class="navbar navbar-expand-lg">
    <div class="container">
        <a class="navbar-brand" href="/">
            <i class="fas fa-paw"></i>
            PetClinic
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav ms-auto">
                <li class="nav-item">
                    <a class="nav-link" href="/">
                        <i class="fas fa-home"></i> ${msg.text("nav.home")}
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/owners/find">
                        <i class="fas fa-search"></i> ${msg.text("nav.findOwners")}
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/vets">
                        <i class="fas fa-user-md"></i> ${msg.text("nav.veterinarians")}
                    </a>
                </li>
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" href="#" id="languageDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                        <i class="fas fa-globe"></i> ${msg.text("nav.language")}
                    </a>
                    <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="languageDropdown">
                        <li><a class="dropdown-item" href="/locale?lang=en&backUrl=${request.getPath()}">${msg.text("lang.en")}</a></li>
                        <li><a class="dropdown-item" href="/locale?lang=es&backUrl=${request.getPath()}">${msg.text("lang.es")}</a></li>
                        <li><a class="dropdown-item" href="/locale?lang=de&backUrl=${request.getPath()}">${msg.text("lang.de")}</a></li>
                    </ul>
                </li>
            </ul>
        </div>
    </div>
</nav>

<main class="py-4">
    ${content}
</main>

<footer class="footer">
    <div class="container">
        <div class="row">
            <div class="col-md-4 mb-4 mb-md-0">
                <h5><i class="fas fa-paw me-2"></i>${msg.text("footer.about")}</h5>
                <p class="mb-0">${msg.text("footer.description")}</p>
            </div>
            <div class="col-md-4 mb-4 mb-md-0">
                <h5>${msg.text("footer.quickLinks")}</h5>
                <ul class="list-unstyled mb-0">
                    <li><a href="/"><i class="fas fa-chevron-right me-2"></i>${msg.text("nav.home")}</a></li>
                    <li><a href="/owners/find"><i class="fas fa-chevron-right me-2"></i>${msg.text("nav.findOwners")}</a></li>
                    <li><a href="/vets"><i class="fas fa-chevron-right me-2"></i>${msg.text("nav.veterinarians")}</a></li>
                </ul>
            </div>
            <div class="col-md-4">
                <h5>${msg.text("footer.poweredBy")}</h5>
                <p class="mb-2">
                    <a href="https://micronaut.io" target="_blank" rel="noopener noreferrer">
                        <i class="fas fa-bolt me-2"></i>${msg.text("footer.micronautFramework")}
                    </a>
                </p>
                <p class="mb-0">
                    <a href="https://github.com/micronaut-projects/micronaut-petclinic" target="_blank" rel="noopener noreferrer">
                        <i class="fab fa-github me-2"></i>${msg.text("footer.viewOnGitHub")}
                    </a>
                </p>
            </div>
        </div>
        <div class="footer-bottom text-center">
            <p class="mb-0">${msg.text("footer.copyright")}</p>
        </div>
    </div>
</footer>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
