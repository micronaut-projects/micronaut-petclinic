# Micronaut Pet Clinic

A Micronaut implementation of the Spring Pet Clinic sample application.

---

## What This Is

A veterinary clinic management system where you can:

- Manage pet owners (create, update, search)
- Register pets for owners
- Schedule veterinary visits
- View veterinarians and their specialties
- Switch between English, Spanish, and German


---

## Requirements

- Java 21 or higher
- Gradle 8+ (or use the included wrapper)
- Docker (optional, for databases)

---

## Quick Start

```bash
# Clone and run with in-memory H2 database
git clone https://github.com/micronaut-projects/micronaut-petclinic.git
cd micronaut-petclinic
./gradlew run
```

Open http://localhost:8080

---

## Running with Different Databases

### Oracle

```bash
docker-compose --profile oracle up
```

> **Note:** The default configuration uses an ARM64 image for Apple Silicon Macs. For x86/AMD64 machines, update the image in `docker-compose.yml` to `container-registry.oracle.com/database/free:latest`.

### MySQL

```bash
docker-compose --profile mysql up
```

### PostgreSQL

```bash
docker-compose --profile postgres up
```

### H2 (In-Memory - Default)

No setup needed. Data is lost when you stop the application.

```bash
./gradlew run
```

---

## Docker Compose

The `docker-compose.yml` file handles everything automatically:
- Starts the database
- Waits for it to be ready
- Starts the application
- Connects them together

To stop:
```bash
docker-compose --profile oracle down   # for Oracle
docker-compose --profile mysql down    # for MySQL
docker-compose --profile postgres down # for PostgreSQL
```

To remove data volumes:
```bash
docker-compose --profile oracle down -v
docker-compose --profile mysql down -v
docker-compose --profile postgres down -v
```

---

## Building a JAR

```bash
# Build
./gradlew build

# Run
java -jar build/libs/micronaut-petclinic-*.jar
```

---

## GraalVM Native Image

If you have GraalVM installed:

```bash
# Build native executable
./gradlew nativeCompile

# Run
./build/native/nativeCompile/micronaut-petclinic
```

---

## How to Use

### Managing Owners

1. Click "FIND OWNERS" in the navigation
2. Click "Add Owner" to register a new owner
3. Fill in the form and submit
4. Search owners by last name using the search form

### Adding Pets

1. Find an owner
2. Click "Add New Pet" on the owner's page
3. Select pet type (dog, cat, bird, etc.) and enter details
4. Submit the form

### Scheduling Visits

1. Go to an owner's page
2. Click "Add Visit" next to one of their pets
3. Enter visit date and description
4. Submit the form

### Viewing Veterinarians

Click "VETERINARIANS" in the navigation to see all vets and their specialties.

### Changing Language

Use the language selector in the top-right corner to switch between:
- English (default)
- Spanish (Español)
- German (Deutsch)

---

## Project Structure

```
src/main/java/
  └── io/micronaut/samples/petclinic/
      ├── model/           # JPA entities (Owner, Pet, Visit, Vet)
      ├── repository/      # Data access interfaces
      ├── service/         # Business logic
      ├── dto/             # Form objects
      ├── controller/      #
      └── system/          #

src/main/resources/
  ├── views/              # Thymeleaf HTML templates
  ├── static/             # CSS and images
  ├── i18n/               # Message translations
  └── application*.yml    # Configuration files
```

---

## Configuration Files

- `application.yml` - Main configuration (H2 default)
- `application-oracle.yml` - Oracle settings
- `application-mysql.yml` - MySQL settings
- `application-postgres.yml` - PostgreSQL settings

To use a specific database locally:
```bash
export MICRONAUT_ENVIRONMENTS=oracle   # for Oracle
export MICRONAUT_ENVIRONMENTS=mysql    # for MySQL
export MICRONAUT_ENVIRONMENTS=postgres # for PostgreSQL
./gradlew run
```

---

## Key Technologies

- **Micronaut 4.x** - Framework
- **Java 21** - Programming language
- **Micronaut Data JPA** - Database access
- **Thymeleaf** - HTML template engine
- **Hibernate** - JPA implementation
- **Caffeine** - Caching
- **Bootstrap 5** - CSS framework

---

## Testing

```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport

# Run integration tests
./gradlew check
```

---

## Migrating from Spring Boot

Main differences you'll encounter:

1. **Dependency Injection**: Use constructor injection, not `@Autowired`
2. **Form Binding**: Add `@Body` annotation to form parameters in controllers
3. **MessageSource**: Must configure manually (not auto-configured)
4. **Templates**: Use OGNL expressions instead of SpEL
5. **Configuration**: Use YAML format, different property names

See [migration-guide.md](migration-guide.md) for detailed comparisons and examples.

---

## Troubleshooting

### Application won't start with Oracle/MySQL/PostgreSQL

Make sure the database is running before starting the app. With docker-compose, this is handled automatically. If running manually:

```bash
# Start database first
docker-compose --profile oracle up -d oracle
docker-compose --profile mysql up -d mysql
docker-compose --profile postgres up -d postgres

# Wait for database to be ready (longer for Oracle, 10s for MySQL/PostgreSQL)
export MICRONAUT_ENVIRONMENTS=oracle  # or mysql, postgres
./gradlew run
```

### Oracle-specific issues

**Image architecture mismatch:** The default Oracle image is for ARM64 (Apple Silicon). For x86/AMD64:
```yaml
# In docker-compose.yml, change:
image: container-registry.oracle.com/database/free:latest
```

**Oracle takes long to start:** Oracle Free needs 2-3 minutes on first startup. The healthcheck waits for it automatically.

### Port 8080 already in use

```bash
# Find what's using the port
lsof -i :8080

# Kill it or use a different port
export MICRONAUT_SERVER_PORT=8081
./gradlew run
```

### Database connection errors

Check your `application-{database}.yml` file has the correct:
- URL
- Username
- Password


---

## Links

- [Micronaut Documentation](https://docs.micronaut.io)
