import gg.jte.ContentType

plugins {
    alias(libs.plugins.micronaut.application)
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

micronaut {
    runtime("netty")
    testRuntime("junit5")
}

dependencies {
    implementation(platform(libs.micronaut.platform.parent))
    annotationProcessor(platform(libs.micronaut.platform.parent))
    testAnnotationProcessor(platform(libs.micronaut.platform.parent))

    implementation(libs.micronaut.http.server.netty)
    implementation(libs.micronaut.http.client)
    // The Deep Data Security profile follows the reference demo's OAuth2
    // login and Azure-backed Oracle JDBC connection setup.
    implementation(libs.micronaut.security.annotations)
    implementation(libs.micronaut.security.jwt)
    implementation(libs.micronaut.security.ojdbc.extensions)
    implementation(libs.micronaut.security.oauth2)
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
    implementation(libs.ojdbc.provider.azure)
    implementation(libs.azure.core.http.jdk.httpclient)
    implementation(libs.oraclepki)
    runtimeOnly(libs.mysql.connector.j)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.snakeyaml)

    annotationProcessor(libs.micronaut.inject.java)
    annotationProcessor(libs.micronaut.security.processor)
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
// The reference demo uses the JDK HTTP transport for the Azure JDBC provider.
// Keep Azure Netty transport off the runtime classpath to avoid competing
// HTTP implementations in the Deep Data Security profile.
configurations.configureEach {
    exclude(group = "com.azure", module = "azure-core-http-netty")
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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    systemProperty("micronaut.server.port", "-1")
}
