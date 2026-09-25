import gg.jte.ContentType
import org.graalvm.buildtools.gradle.tasks.NativeRunTask

plugins {
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.micronaut.test.resources)
    jacoco
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.jte)
}

group = providers.gradleProperty("projectGroup").orElse("io.micronaut.samples").get()
version = providers.gradleProperty("projectVersion").orElse("1.0.0-SNAPSHOT").get()

repositories {
    mavenCentral()
}

application {
    mainClass.set("io.micronaut.samples.petclinic.Application")
}

data class DatabaseIntegration(
    val testTaskName: String,
    val environment: String,
    val descriptionName: String,
)

val databaseIntegrations = listOf(
    DatabaseIntegration(
        testTaskName = "testMysqlIntegration",
        environment = "mysql",
        descriptionName = "MySQL",
    ),
    DatabaseIntegration(
        testTaskName = "testPostgresIntegration",
        environment = "postgres",
        descriptionName = "PostgreSQL",
    ),
    DatabaseIntegration(
        testTaskName = "testOracleIntegration",
        environment = "oracle",
        descriptionName = "Oracle DB",
    )
)

val testResourcesClientTimeoutSeconds = 180

micronaut {
    runtime("netty")
    testRuntime("junit5")
    testResources {
        enabled = true
        clientTimeout.set(testResourcesClientTimeoutSeconds)
    }
}

dependencies {
    implementation(platform(libs.micronaut.platform.parent))
    annotationProcessor(platform(libs.micronaut.platform.parent))
    testAnnotationProcessor(platform(libs.micronaut.platform.parent))

    implementation(libs.micronaut.http.server.netty)
    implementation(libs.micronaut.serde.jackson)
    implementation(libs.micronaut.views.jte)
    implementation(libs.micronaut.data.jdbc)
    implementation(libs.micronaut.jdbc.hikari)
    implementation(libs.micronaut.validation)
    implementation(libs.jakarta.validation.api)
    implementation(libs.micronaut.cache.caffeine)
    implementation(libs.micronaut.sourcegen.annotations)
    implementation(libs.micronaut.managment)

    runtimeOnly(libs.h2)
    runtimeOnly(libs.h2gis)
    runtimeOnly(libs.ojdbc11)
    runtimeOnly(libs.mysql.connector.j)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.snakeyaml)

    annotationProcessor(libs.micronaut.inject.java)
    testAnnotationProcessor(libs.micronaut.inject.java)
    annotationProcessor(libs.micronaut.data.processor)
    annotationProcessor(libs.micronaut.validation.processor)
    annotationProcessor(libs.micronaut.serde.processor)
    annotationProcessor(libs.micronaut.sourcegen.generator.java)
    testAnnotationProcessor(libs.micronaut.data.processor)
    testAnnotationProcessor(libs.micronaut.validation.processor)
    testAnnotationProcessor(libs.micronaut.serde.processor)
    testAnnotationProcessor(libs.micronaut.sourcegen.generator.java)

    testImplementation(libs.micronaut.test.junit5)
    testImplementation(libs.micronaut.http.client)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj.core)
    jteGenerate(libs.jte.native.resources)
}

jte {
    sourceDirectory = file("src/main/resources/views").toPath()
    contentType = ContentType.Html
    binaryStaticContent = true
    jteExtension("gg.jte.nativeimage.NativeResourcesExtension")
    generate()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.compilerArgs.addAll(
        listOf(
            "-Amicronaut.processing.group=io.micronaut.samples",
            "-Amicronaut.processing.module=micronaut-petclinic"
        )
    )
}

tasks.named("inspectRuntimeClasspath") {
    dependsOn(tasks.named("generateJte"))
}

tasks.named("check").configure {
    databaseIntegrations.forEach { integration ->
        dependsOn(tasks.named(integration.testTaskName))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    systemProperty("micronaut.server.port", "-1")
    applyDefaultEnvironment()
}

tasks.named<NativeRunTask>("nativeTest") {
    if (defaultMicronautEnvironment()) {
        environment.put("MICRONAUT_ENVIRONMENTS", "test,h2")
    }
}

databaseIntegrations.forEach { integration ->
    tasks.register<Test>(integration.testTaskName) {
        group = "verification"
        description =
            "Runs tests against a disposable ${integration.descriptionName} database from Micronaut Test Resources."
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        systemProperty("micronaut.environments", "test,${integration.environment}")
    }
}

tasks.named<JavaExec>("run") {
    applyDefaultEnvironment()
}

fun defaultMicronautEnvironment(): Boolean =
    System.getProperty("micronaut.environments").isNullOrBlank() &&
            System.getenv("MICRONAUT_ENVIRONMENTS").isNullOrBlank()

fun JavaForkOptions.applyDefaultEnvironment(environment: String = "h2") {
    if (defaultMicronautEnvironment()) {
        systemProperty("micronaut.environments", environment)
    }
}

