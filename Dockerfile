# Build stage
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Install curl (needed by Maven/Gradle wrappers)
RUN apk add --no-cache curl

# Maven build (default)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests

# Gradle build (alternative)
# To use it, comment out the Maven block above and uncomment these lines.
#
# COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./
# COPY gradle gradle
# RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
# COPY src src
# RUN ./gradlew build -x test --no-daemon

# Run stage (JVM)
FROM eclipse-temurin:25-jre-alpine AS jvm
WORKDIR /app
COPY --from=build /app/target/micronaut-petclinic-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
