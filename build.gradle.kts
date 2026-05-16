plugins {
    alias(libs.plugins.micronaut.application)
    jacoco
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.jte.gradle)
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
    implementation(libs.micronaut.serde.jackson)
    implementation(libs.micronaut.views.jte)
    implementation(libs.micronaut.data.jdbc)
    implementation(libs.micronaut.jdbc.hikari)
    implementation(libs.micronaut.validation)
    implementation(libs.jakarta.validation.api)
    implementation(libs.micronaut.cache.caffeine)
    implementation(libs.micronaut.sourcegen.annotations)

    runtimeOnly(libs.h2)
    runtimeOnly(libs.ojdbc11)
    runtimeOnly(libs.mysql.connector.j)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.snakeyaml)

    compileOnly(libs.micronaut.inject.java)
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
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.compilerArgs.addAll(
        listOf(
            "-Amicronaut.processing.group=io.micronaut.samples",
            "-Amicronaut.processing.module=micronaut-petclinic"
        )
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    systemProperty("micronaut.server.port", "-1")
}

jte {
    sourceDirectory.set(file("src/main/resources/views").toPath())
    contentType.set(gg.jte.ContentType.Html)
    generate()
}

tasks.named("inspectRuntimeClasspath") {
    dependsOn(tasks.named("generateJte"), generateJteNativeConfig)
}

// Generate reflect-config.json for JTE precompiled template classes so GraalVM native image
// can load them dynamically via ClassLoader.loadClass() and invoke render() via reflection.
val jteNativeConfigBaseDir = layout.buildDirectory.dir("generated-resources/jte-native-image")
val jteNativeConfigOutputDir = jteNativeConfigBaseDir.map {
    it.dir("META-INF/native-image/io.micronaut.samples/micronaut-petclinic")
}

val generateJteNativeConfig by tasks.registering {
    dependsOn(tasks.named("compileJava"))
    inputs.dir(layout.buildDirectory.dir("classes/java/main/gg/jte/generated/precompiled"))
    outputs.dir(jteNativeConfigBaseDir)

    doLast {
        val classesDir = layout.buildDirectory.dir("classes/java/main").get().asFile
        val jteClasses = fileTree(classesDir) {
            include("gg/jte/generated/precompiled/**/*.class")
            exclude("**/*\$*.class") // exclude anonymous/inner classes
        }

        val entries = jteClasses.files.map { file ->
            val className = file.relativeTo(classesDir)
                .path
                .replace(File.separatorChar, '.')
                .removeSuffix(".class")
            """  {
    "name": "$className",
    "allDeclaredMethods": true,
    "allDeclaredFields": true
  }"""
        }

        val json = "[\n${entries.joinToString(",\n")}\n]\n"
        val outputFile = jteNativeConfigOutputDir.get().file("reflect-config.json").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json)
    }
}

sourceSets["main"].resources.srcDir(jteNativeConfigBaseDir)

tasks.named("processResources") {
    dependsOn(generateJteNativeConfig)
}
