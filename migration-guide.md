# Migrating Spring Pet Clinic to Micronaut

This guide covers the migration of Spring Pet Clinic to Micronaut Framework, focusing on the differences that matter and the problems that require actual work to solve.

---

## Core Framework Differences

### Dependency Injection

Spring Boot uses runtime reflection. Micronaut generates dependency injection code at compile time.

**Spring Boot:**
```java
@Service
public class OwnerService {
    @Autowired
    private OwnerRepository repository;
}
```

**Micronaut:**
```java
@Singleton
public class OwnerService {
    private final OwnerRepository repository;
    
    public OwnerService(OwnerRepository repository) {
        this.repository = repository;
    }
}
```

Use constructor injection in Micronaut. Field injection and `@Inject` are not recommended.

### Controller Return Types

**Spring Boot:**
```java
@GetMapping("/owners/{id}")
public String showOwner(@PathVariable Long id, Model model) {
    model.addAttribute("owner", owner);
    return "owners/details";
}
```

**Micronaut:**
```java
@Get("/owners/{id}")
public HttpResponse<ModelAndView> showOwner(@PathVariable Long id) {
    return HttpResponse.ok(new ModelAndView<>(
        "owners/details",
        Map.of("owner", owner)
    ));
}
```

Micronaut requires explicit model creation and HttpResponse wrapping. This provides more control but requires more code.

### Configuration Approach

Spring Boot provides extensive auto-configuration. Micronaut requires explicit configuration for most features.

Example - MessageSource configuration:

```java
@Factory
public class MessageSourceFactory {
    @Singleton
    public MessageSource createMessageSource() {
        return new ResourceBundleMessageSource("i18n.messages");
    }
}
```

This factory bean is required in Micronaut. Spring Boot creates it automatically.

---

## Form Binding

### The Problem

Spring Boot automatically binds form data to entities using reflection:

```java
@PostMapping("/owners/new")
public String create(@Valid Owner owner, BindingResult result) {
    // Spring binds form fields to Owner entity
}
```

Micronaut requires explicit Data Transfer Objects (DTOs) with compile-time introspection:

```java
@Introspected  // Required for compile-time binding
public class OwnerForm {
    @NotEmpty
    private String firstName;
    
    @NotEmpty
    private String lastName;
    
    @NotEmpty
    private String address;
    
    @NotEmpty
    private String city;
    
    @NotEmpty
    @Pattern(regexp = "\\d{10}")
    private String telephone;
    
    // Constructor, getters, setters
    
    public Owner toOwner() {
        Owner owner = new Owner();
        owner.setFirstName(this.firstName);
        owner.setLastName(this.lastName);
        owner.setAddress(this.address);
        owner.setCity(this.city);
        owner.setTelephone(this.telephone);
        return owner;
    }
}
```

### Controller Usage

```java
@Post("/owners/new")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public HttpResponse<?> create(@Valid @Body OwnerForm form) {
    Owner owner = form.toOwner();
    ownerService.save(owner);
    return HttpResponse.redirect(URI.create("/owners/" + owner.getId()));
}
```

### Why DTOs Are Required

Micronaut uses compile-time introspection instead of runtime reflection. The `@Introspected` annotation generates bean metadata at compile time. This approach:

- Eliminates reflection overhead
- Provides type safety
- Enables better separation between forms and entities
- Supports GraalVM native image compilation

The migration required creating DTOs for all forms: `OwnerForm`, `PetForm`, `VisitForm`.

---

## Internationalization Implementation

Spring Boot provides auto-configured internationalization. Micronaut requires manual setup for MessageSource, LocaleResolver, and language switching.

### MessageSource Configuration

Create a factory bean for MessageSource:

```java
@Factory
public class MessageSourceFactory {
    @Singleton
    public MessageSource createMessageSource() {
        return new ResourceBundleMessageSource("i18n.messages");
    }
}
```

Without this factory, message keys will appear as `??key??` in templates.

### Message Properties

Create property files in `src/main/resources/i18n/`:

**messages.properties (English):**
```properties
nav.home=Home
nav.findOwners=Find Owners
owner.firstName=First Name
owner.lastName=Last Name
button.save=Save
```

**messages_es.properties (Spanish):**
```properties
nav.home=Inicio
nav.findOwners=Buscar Propietarios
owner.firstName=Nombre
owner.lastName=Apellido
button.save=Guardar
```

**messages_de.properties (German):**
```properties
nav.home=Startseite
nav.findOwners=Besitzer Suchen
owner.firstName=Vorname
owner.lastName=Nachname
button.save=Speichern
```

The implementation uses 250+ message keys across 3 languages.

### Locale Resolution

Implement a custom locale resolver:

```java
@Singleton
@Replaces(HttpLocaleResolver.class)
public class CookieLocaleResolver implements HttpLocaleResolver {
    
    private static final String LOCALE_COOKIE_NAME = "locale";
    
    @Override
    public Optional<Locale> resolve(HttpRequest<?> request) {
        // Check cookie first
        Optional<Cookie> cookie = request.getCookies().findCookie(LOCALE_COOKIE_NAME);
        if (cookie.isPresent()) {
            String lang = cookie.get().getValue();
            if (StringUtils.isNotEmpty(lang)) {
                return Optional.of(Locale.forLanguageTag(lang));
            }
        }
        
        // Fall back to Accept-Language header
        return request.getLocale();
    }
    
    @Override
    public Locale resolveOrDefault(HttpRequest<?> request) {
        return resolve(request).orElse(Locale.ENGLISH);
    }
}
```

### Language Switching Controller

```java
@Controller("/locale")
public class LocaleController {
    
    @Get
    public HttpResponse<?> changeLocale(@QueryValue(defaultValue = "en") String lang,
                                        HttpRequest<?> request) {
        // Validate language
        String validLang = switch (lang.toLowerCase()) {
            case "es" -> "es";
            case "de" -> "de";
            default -> "en";
        };
        
        // Create cookie with 1 year expiration
        Cookie cookie = Cookie.of("locale", validLang)
                .maxAge(Duration.ofDays(365))
                .path("/");
        
        // Redirect to referrer
        String referer = request.getHeaders().get("Referer").orElse("/");
        
        return HttpResponse.redirect(URI.create(referer)).cookie(cookie);
    }
}
```

### Template Updates

Replace hard-coded text with message keys:

**Before:**
```html
<a href="/">Home</a>
<button type="submit">Save</button>
```

**After:**
```html
<a th:href="@{/}" th:text="#{nav.home}">Home</a>
<button type="submit" th:text="#{button.save}">Save</button>
```

### Language Selector Component

```html
<li class="nav-item dropdown">
    <a class="nav-link dropdown-toggle" href="#" data-bs-toggle="dropdown">
        <i class="fas fa-globe"></i>
        <span th:if="${#locale.toString() == 'en'}">English</span>
        <span th:if="${#locale.toString() == 'es'}">Español</span>
        <span th:if="${#locale.toString() == 'de'}">Deutsch</span>
    </a>
    <ul class="dropdown-menu">
        <li><a class="dropdown-item" th:href="@{/locale(lang='en')}">English</a></li>
        <li><a class="dropdown-item" th:href="@{/locale(lang='es')}">Español</a></li>
        <li><a class="dropdown-item" th:href="@{/locale(lang='de')}">Deutsch</a></li>
    </ul>
</li>
```

Users select a language, receive a cookie, and all subsequent requests render in that language.

---

## Common Migration Issues

### HttpServletRequest Not Available in Templates

**Problem:**
```html
<input type="hidden" name="referer" th:value="${#httpServletRequest.requestURI}"/>
```

**Error:**
```
Exception evaluating OGNL expression: "#httpServletRequest.requestURI"
```

**Cause:** Spring exposes HttpServletRequest to templates automatically. Micronaut does not.

**Solution:** Use the HTTP Referer header:
```java
String referer = request.getHeaders().get("Referer").orElse("/");
```

### Locale Cookie Name Mismatch

**Problem:** Locale preferences not persisting between requests.

**Cause:** Cookie name mismatch between controller (`"locale"`) and resolver (`"LOCALE"`). String comparison is case-sensitive.

**Solution:** Use a shared constant:
```java
private static final String LOCALE_COOKIE_NAME = "locale";
```

### Query Generation Differences

**Problem:** Spring Data JPA queries not working in Micronaut Data.

**Spring Boot:**
```java
@Query("SELECT DISTINCT owner FROM Owner owner LEFT JOIN FETCH owner.pets")
List<Owner> findAll();
```

**Solution:** Use Micronaut Data's `@Join` annotation:
```java
@Join("pets")
List<Owner> findAll();
```

Micronaut Data uses compile-time query generation with different patterns than Spring Data JPA.

### Missing Message Keys

**Problem:** Templates display `??welcome??` instead of translated text.

**Cause:** MessageSource not configured.

**Solution:** Verify the MessageSourceFactory bean exists and returns a properly configured ResourceBundleMessageSource.

---

## Configuration Differences

### Spring (application.properties)

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/petclinic
spring.datasource.username=petclinic
spring.datasource.password=petclinic
spring.jpa.hibernate.ddl-auto=update
```

### Micronaut (application.yml)

```yaml
datasources:
  default:
    url: jdbc:mysql://localhost:3306/petclinic
    username: petclinic
    password: petclinic
    driver-class-name: com.mysql.cj.jdbc.Driver
    
jpa:
  default:
    properties:
      hibernate:
        hbm2ddl:
          auto: update
```

Different structure, but easy enough to convert.

---

## Key Learnings

### Form Handling

Create DTOs with `@Introspected` from the start. Attempting to bind directly to entities will not work.

### Micronaut Data Patterns

The `@Join` annotation is more effective than JPQL for most association fetching use cases. Review Micronaut Data documentation for query patterns.

### Configuration

Budget time for creating factory beans. Features that Spring Boot auto-configures require explicit setup in Micronaut.

### Testing

Compile-time checks do not catch template errors or missing message keys. Thorough runtime testing is required.

### Dependency Injection

Constructor injection is the standard pattern. Avoid field injection and `@Inject` annotations.
