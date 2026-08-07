# Migrating Micronaut PetClinic from Micronaut 4 to Micronaut 5

This document records the project-specific steps applied on this branch to move Micronaut PetClinic from Micronaut Platform 4.10.x to Micronaut Platform 5.x.x.

## Baseline

The migration branch was based on commit `4907206`, where the project was using:

- Micronaut Platform `4.10.16`
- Micronaut Gradle plugin `4.6.2`
- Micronaut SourceGen `1.8.5`
- Java 21 for Gradle compilation and Docker images, with CI also testing Java 25

## 1. Update Micronaut Platform versions

Both build systems were updated to Micronaut Platform `5.0.5`.

Gradle changes:

- `gradle/libs.versions.toml`
  - `micronaut-platform`: `4.10.16` -> `5.0.5`
  - `micronaut-gradle-plugin`: `4.6.2` -> `5.0.2`
  - `micronaut-sourcegen`: `1.8.5` -> `2.1.0`

Maven changes:

- `pom.xml`
  - parent `io.micronaut.platform:micronaut-parent`: `4.10.16` -> `5.0.5`
  - `micronaut.version`: `4.10.16` -> `5.0.5`
  - `micronaut.sourcegen.version`: `1.8.5` -> `2.1.0`

## 2. Align Java level to Java 25

The project was aligned on Java 25 for the Micronaut 5 branch.

Changes applied:

- `build.gradle.kts`
  - `JavaCompile.options.release`: `21` -> `25`
- `Dockerfile`
  - build image: `eclipse-temurin:21-jdk-alpine` -> `eclipse-temurin:25-jdk-alpine`
  - runtime image: `eclipse-temurin:21-jre-alpine` -> `eclipse-temurin:25-jre-alpine`
- GitHub Actions workflows
  - `.github/workflows/gradle.yml`
  - `.github/workflows/graalvm-native.yml`
  - `.github/workflows/maven.yml`
  - `.github/workflows/maven-native.yml`
  - CI matrix reduced from `['21', '25']` to `['25']`

The Maven build already had:

```xml
<jdk.version>25</jdk.version>
<release.version>25</release.version>
```

## 3. Update SourceGen for Micronaut 5 compatibility

Micronaut SourceGen was upgraded from `1.8.5` to `2.1.0`.

This affects both:

- compile-time annotations: `micronaut-sourcegen-annotations`
- annotation processor: `micronaut-sourcegen-generator-java`

The Gradle and Maven builds continue to configure SourceGen as an annotation processor.

## 4. Update ViewModelProcessor implementation for Micronaut Views API changes

`I18nViewModelProcessor` was updated for the Micronaut 5 / Micronaut Views API shape.

File:

- `src/main/java/io/micronaut/samples/petclinic/system/I18nViewModelProcessor.java`

Changes:

- Updated the interface from:

```java
ViewModelProcessor<Object>
```

to:

```java
ViewModelProcessor<Object, HttpRequest<?>>
```

- Replaced Micronaut’s `io.micronaut.core.annotation.NonNull` import with JSpecify:

```java
org.jspecify.annotations.NonNull
```

This keeps the processor compatible with the newer method signatures and nullability annotations used by Micronaut 5 dependencies.

## 5. Update Maven wrapper

The Maven wrapper configuration was updated.

File:

- `.mvn/wrapper/maven-wrapper.properties`

Changes:

- Added `wrapperVersion=3.3.4`
- Added `distributionType=only-script`
- Kept Maven distribution `apache-maven-3.9.16`
- Removed the old explicit `wrapperUrl` for Maven Wrapper `3.2.0`

The `mvnw` script was regenerated as part of this update.

## 6. Native-image configuration for Java 25

Native-image configuration for the Java 25 / Micronaut 5 combination is kept aligned with the standard Micronaut and GraalVM plugin setup.

Maven native configuration in `pom.xml` mirrors the runtime-initialization and Netty native-image exclusions, and the native Maven plugin was updated:

- `org.graalvm.buildtools:native-maven-plugin`: `1.1.2` -> `1.1.5`

## 7. Docker Compose cleanup

`docker-compose.yml` no longer declares:

```yaml
version: '3.8'
```

This removes the obsolete Compose file version declaration.

## Verification

Use these commands to verify the migration:

```bash
./gradlew test
./mvnw -B -ntp verify
./mvnw -B -ntp -Pnative -Djdk.version=25 -Drelease.version=25 test
```

The branch also keeps dedicated CI coverage for:

- Gradle JVM tests on Java 25
- Maven JVM verification on Java 25
- Gradle native tests on Java 25
- Maven native tests on Java 25
