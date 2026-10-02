# Micronaut Pet Clinic

Micronaut PetClinic sample application built with Micronaut 5.

A modern **Micronaut PetClinic example** and implementation of the classic Spring PetClinic, demonstrating how to build fast, cloud-native Java applications using the Micronaut framework.

---

## What is Micronaut PetClinic?

Micronaut PetClinic is a **reference Micronaut application** that showcases how to build a real-world veterinary clinic system using Java and Micronaut.

This Micronaut PetClinic app allows you to:

- Manage pet owners (create, update, search)
- Register pets for owners
- Schedule veterinary visits
- View veterinarians and their specialities
- Switch between English, Spanish, and German


---

## Requirements

- Java 25 or higher
- Maven 3.9+ (or use the included wrapper)
- Gradle 9+ (or use the included wrapper)
- Docker (optional, for databases)

---

## Quick Start

```bash
# Clone and run with in-memory H2 database (Maven)
git clone https://github.com/micronaut-projects/micronaut-petclinic.git
cd micronaut-petclinic

./mvnw mn:run

# Gradle alternative
./gradlew run
```

Open http://localhost:8080

---

## Running with Different Databases

### Oracle

```bash
cp .env.example .env
# Set ORACLE_DB_PASSWORD in .env, then:
set -a && source .env && set +a
docker-compose --profile oracle up
```

> **Note:** The Oracle profile uses the full Oracle AI Database 26ai Free image so it can configure TCPS. Set `ORACLE_IMAGE` if you need a different compatible image for your platform.

### Oracle Deep Data Security showcase

The repository also includes an opt-in Oracle Deep Data Security integration. It connects Micronaut Data JDBC through the Micronaut Security OJDBC extension, propagating the authenticated end-user access token to Oracle. Oracle maps Entra app roles to data roles and applies `DATA GRANT` policies at the row and column boundary.

The showcase mirrors the reference demo's Oracle path: Micronaut OAuth2 browser login with a signed application cookie, the Micronaut Security OJDBC end-user-context extension, an Entra service-principal JDBC token, and TCPS wallet settings. It requires an Oracle AI Database 26ai / 23.26.x-compatible image, an IAM provider, and two OAuth client flows: the incoming delegated end-user access token and the application's client-credentials database-access token. The demo maps the Entra `PET_OWNER` and `CLINIC_STAFF` app roles to Oracle data roles and filters/masks `OWNERS` by the signed-in user's role and email/UPN.

Follow [`docker/oracle/deepsec/README.md`](docker/oracle/deepsec/README.md) for the setup sequence. The short version is:

```bash
# Start Oracle, initialize the schema/data if needed, and apply DeepSec grants.
docker-compose --profile oracle-deepsec up -d

# Export the generated TCPS wallet for the host-launched application.
sh docker/oracle/export-wallet.sh

# Start the application from the terminal after loading .env.
MICRONAUT_ENVIRONMENTS=oracle-deepsec ./gradlew clean run --no-daemon

# Open the application in a browser and choose the Entra login link.
open http://localhost:8080/

# After login, Micronaut redirects to the Oracle Deep Sec query.
open http://localhost:8080/deepsec/owners
```

The DeepSec Compose setup runs the SQL migration in
[`docker/oracle/deepsec/00-initialize-petclinic.sql`](docker/oracle/deepsec/00-initialize-petclinic.sql)
before applying the Oracle security policy and optional identity fixture. The
normal PetClinic endpoints remain unchanged. The showcase endpoints are
available only in the `oracle-deepsec` environment:

- `GET /deepsec/owners` renders the owner cards and role-escalation explanation. A user with the Entra `PET_OWNER` role sees only the owner row whose `EMAIL` matches their UPN; the `CLINIC_STAFF` persona sees the expanded owner set.
- `GET /deepsec/owners.json` returns the same Oracle-filtered result as JSON.

The DeepSec page logs out through `/oauth/logout`, which signs the user out of
Microsoft Entra ID and then clears the local Micronaut cookie. Register
`http://localhost:8080/logout` as a Web post-logout redirect URI in the PetClinic
app registration.

The support-contact demonstration uses a local Oracle `PETCLINIC_SUPPORT` data
role. It is authorized for the application identity but disabled by default;
the `@RunAs` repository method enables it only for that operation. It does not
need to be assigned to an Entra user.

The pet-owner/clinic-staff row policy is generic: `PET_OWNER` compares the
owner row's `EMAIL` value with `ORA_END_USER_CONTEXT.username`, while
`CLINIC_STAFF` has no row predicate and can see the full owner set. The two
email values in `.env` are only used by the optional demo fixture to associate
sample rows with the two test identities.

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
# Maven
./mvnw mn:run

# Gradle alternative
./gradlew run
```

---

## Docker Compose

The `docker-compose.yml` file manages the database containers and waits for
their health checks. The standard application profiles can be started as
separate services; the Oracle Deep Data Security application is launched from
the terminal so it can use the host's Entra credentials and exported TCPS
wallet.

> **Note:** The repository supports both Maven and Gradle for local development. The `Dockerfile` uses Maven for the container image.

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
./mvnw package

# Run
java -jar target/micronaut-petclinic-*.jar

# Gradle alternative
./gradlew build
java -jar build/libs/micronaut-petclinic-*.jar
```

---

## GraalVM Native Image

If you have GraalVM installed:

```bash
# Build native executable (Maven)
./mvnw package -Pnative

# Run
./target/micronaut-petclinic

# Gradle alternative
./gradlew nativeCompile
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

Click "VETERINARIANS" in the navigation to see all vets and their specialities.

### Changing Language

Use the language selector in the top-right corner to switch between:
- English (default)
- Spanish (Español)
- German (Deutsch)

## Project Structure

```
src/main/java/
  └── io/micronaut/samples/petclinic/
      ├── model/           # Micronaut Data JDBC entities (Owner, Pet, Visit, Vet)
      ├── repository/      # Data access interfaces
      ├── service/         # Business logic
      ├── dto/             # Form objects
      ├── controller/      #
      └── system/          #

src/main/resources/
  ├── views/              # JTE templates
  ├── static/             # CSS and images
  ├── i18n/               # Message translations
  └── application*.yml    # Configuration files
```

---

## Configuration Files

- `application.yml` - Main configuration (H2 default)
- `application-oracle.yml` - Oracle settings
- `application-oracle-deepsec.yml` - opt-in Oracle Deep Data Security and IAM settings
- `application-mysql.yml` - MySQL settings
- `application-postgres.yml` - PostgreSQL settings

To use a specific database locally:
```bash
export MICRONAUT_ENVIRONMENTS=oracle   # for Oracle
export MICRONAUT_ENVIRONMENTS=mysql    # for MySQL
export MICRONAUT_ENVIRONMENTS=postgres # for PostgreSQL

# Maven
./mvnw mn:run

# Gradle alternative
./gradlew run
```

---

## Key Technologies

- **Micronaut 5.x** - Framework
- **Java 25** - Programming language
- **Micronaut Data JDBC** - Database access
- **JTE** - HTML template engine
- **HikariCP** - JDBC connection pooling
- **Caffeine** - Caching
- **Bootstrap 5** - CSS framework

---

## Testing

`./mvnw test` runs the integration suite against the default in-memory H2 database. `./mvnw verify` runs that H2 suite first, then reruns it against disposable PostgreSQL, MySQL, and Oracle databases managed by Micronaut Test Resources.

```bash
# H2 integration suite
./mvnw test

# H2 plus PostgreSQL, MySQL, and Oracle integration suites
./mvnw verify
```

Docker must be running for `verify`; Micronaut Test Resources provisions the disposable database containers and pulls missing images automatically. Oracle startup is typically slower than PostgreSQL and MySQL.

```bash
# Gradle equivalents
./gradlew test                  # H2 integration suite
./gradlew check                 # H2 plus all database integrations

# Run one database integration suite
./gradlew testPostgresIntegration
./gradlew testMysqlIntegration
./gradlew testOracleIntegration

# Generate coverage
./gradlew test jacocoTestReport
```

The former Maven database test profiles are no longer used.

`OracleTransactionPriorityIntegrationTest` and `OracleTransactionPriorityControllerTest` require `MICRONAUT_ENVIRONMENTS=oracle`; the default `CREATE_DROP` setting drops and recreates application tables in the `petclinic` schema.
Use a disposable database, or preserve an already-seeded schema with `DATASOURCES_DEFAULT_SCHEMA_GENERATE=NONE PETCLINIC_SAMPLE_DATA_ENABLED=false`.

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

## Features

### Geospatial Clinic Search

The application also includes a Micronaut Data geospatial example. It stores sample clinic branches as WGS 84 `Point` values (SRID 4326) and exposes three derived repository methods through `ClinicRepository`: `findByLocationNear`, `findByLocationGeoWithin`, and `findByLocationGeoIntersects`. Micronaut Data translates those derived methods to the spatial functions/operators of the active dialect. For example, `Near` is compiled to Oracle `SDO_WITHIN_DISTANCE` when the Oracle profile is active.

Open http://localhost:8080/clinics to try the clinic search page.

Use the manual form or the map tab to search clinic locations. Use `nearby` for radius searches around a single point, `within` for clinics inside a bounding-box or drawn polygon, and `intersects` for clinics whose location intersects an open `LineString`. Using a line for `intersects` makes the example distinct from `within`, which uses a filled `Polygon`.

```bash
curl -X POST http://localhost:8080/clinics/nearby \
  -H "Content-Type: application/json" \
  -d '{"latitude":43.0731,"longitude":-89.4012,"radiusMeters":5000}'

curl -X POST http://localhost:8080/clinics/within \
  -H "Content-Type: application/json" \
  -d '{"coordinates":[{"latitude":43.0000,"longitude":-89.5500},{"latitude":43.2000,"longitude":-89.5500},{"latitude":43.2000,"longitude":-89.2000},{"latitude":43.0000,"longitude":-89.2000},{"latitude":43.0000,"longitude":-89.5500}]}'

curl -X POST http://localhost:8080/clinics/intersects \
  -H "Content-Type: application/json" \
  -d '{"coordinates":[{"latitude":43.0753,"longitude":-89.5186},{"latitude":43.1020,"longitude":-89.3545},{"latitude":43.1836,"longitude":-89.2137}]}'
```
---

### Oracle semantic chunk retrieval

The Oracle profile also includes a retrieval-only vector search example based on Micronaut Data's [vector type support](https://github.com/micronaut-projects/micronaut-data/pull/3637). It seeds a small pet-care knowledge base, stores each chunk as a `FloatVector` in an Oracle `VECTOR(384, FLOAT32)` column, and uses the derived vector-search repository method with cosine distance.

Start the Oracle profile and open http://localhost:8080/knowledge. The demo uses vectors precomputed once with the all-MiniLM-L6-v2 model and checked into `src/main/resources/knowledge/pet-care-embeddings.tsv`; the runtime has no embedding model, ONNX Runtime, native tokenizer, LLM, or external API key. It returns ranked chunks with their source, topic, species, and distance. The HTTP API is:

```bash
curl -X POST http://localhost:8080/knowledge/search \
  -H "Content-Type: application/json" \
  -d '{"query":"What vaccinations does my puppy need?"}'
```

The vector service is intentionally an interface, making it straightforward to replace the checked-in catalog with another vector source while keeping Oracle retrieval unchanged. The sample query vectors are cataloged alongside the chunk vectors, so queries outside the demo catalog return no matches.

### Oracle transaction priority

With the Oracle profile running, open http://localhost:8080/oracle/transaction-priority.
Sample data provides two appointments, without calendar or time-slot management.
The page lists all available appointments in display order.

1. Choose an available appointment and start **regular booking (LOW)**. It locks
   the row and pauses for 15 seconds to simulate checkout.
2. Start **emergency booking (HIGH)** during that pause. With the Docker settings,
   Oracle can roll back LOW after HIGH waits about 3 seconds, letting HIGH commit.
3. LOW reports the rollback when its pause ends and it tries to save again.
   The booked appointment disappears from the choices; retry with the remaining one.
   Without HIGH, LOW commits normally after its pause.

Each button sends an independent request to an `@OracleTransactional` method.
`SELECT … FOR UPDATE` acquires the lock; `save()` persists changes inside that
transaction, without committing it. `WAIT 10` limits lock acquisition to 10 seconds,
not how long the lock is held. Both transactions have a 30-second timeout.
The countdown is approximate; the pause is demo-only, not a production booking pattern.
The booking request is `POST /oracle/transaction-priority/book?appointmentId=ID&type=regular|emergency`.

Micronaut Data 5.2 translates Oracle's `ORA-63300` / `ORA-63302` errors into
`OracleTransactionPriorityException`, which the controller maps to HTTP 409 Conflict.
HIGH arriving before LOW locks the row, or too late to displace LOW, does not
demonstrate it. A timeout is not proof of priority rollback, and a committed
booking cannot be displaced.

[The Oracle startup script](docker/oracle/01-init-user.sql) sets
`PRIORITY_TXNS_MODE=ROLLBACK` and the HIGH/MEDIUM wait targets to 3 seconds.
To apply script changes to an existing container, restart it without deleting volumes:

```bash
docker compose --profile oracle restart oracle
docker compose --profile oracle logs oracle
```

Check for `Oracle Priority Transactions enabled`; unsupported images and setup
errors are reported in the startup logs.

Use one browser and a disposable database. **Reset currently makes every appointment
available**, not just the two demo rows. The UI disables reset while its requests run.
Wait for bookings in any other tabs or clients to finish too. Reset uses plain
`@Transactional`, so it runs at Oracle's default HIGH priority. With priority
rollback enabled, a reset blocked by a LOW booking can cause Oracle to roll that
booking back after the configured 3-second HIGH wait target. Reset can also clear
a booking that commits while it waits for the row lock.

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

# Maven
./mvnw mn:run

# Gradle alternative
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

# Maven
./mvnw mn:run

# Gradle alternative
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
