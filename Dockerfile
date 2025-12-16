# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Install curl (needed by Maven wrapper)
RUN apk add --no-cache curl

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw package -DskipTests

# Run stage (JVM)
FROM eclipse-temurin:21-jre-alpine AS jvm
WORKDIR /app
COPY --from=build /app/target/micronaut-petclinic-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]