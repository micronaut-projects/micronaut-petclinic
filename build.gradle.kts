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
    implementation(libs.micronaut.serde.jackson)
    implementation(libs.micronaut.views.jte)
    implementation(libs.micronaut.data.jdbc)
    implementation(libs.micronaut.jdbc.hikari)
    implementation(libs.micronaut.validation)
    implementation(libs.jakarta.validation.api)
    implementation(libs.micronaut.cache.caffeine)
    implementation(libs.micronaut.sourcegen.annotations)
    implementation(libs.micronaut.managment)
    implementation(libs.langchain4j.embeddings.all.minilm.l6.v2)
    implementation(libs.micronaut.email.javamail)

    runtimeOnly(libs.h2)
    runtimeOnly(libs.h2gis)
    runtimeOnly(libs.ojdbc11)
    runtimeOnly(libs.mysql.connector.j)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.snakeyaml)
    runtimeOnly(libs.eclipse.angus.mail)

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

graalvmNative {
    binaries {
        named("main") {
            buildArgs.add("--enable-native-access=ALL-UNNAMED")
            buildArgs.add("--exclude-config")
            buildArgs.add(".*micronaut-http-netty-[^/]+\\.jar")
            buildArgs.add("^/META-INF/native-image/io\\.micronaut\\.micronaut\\.http\\.netty/native-image\\.properties$")
            buildArgs.add("--initialize-at-run-time=io.netty.util.internal.CleanerJava25")
            buildArgs.add("--initialize-at-run-time=sun.security.util.Password\$ConsoleHolder")
            buildArgs.add("--initialize-at-run-time=jdk.internal.io.JdkConsoleImpl\$1ConsoleHolder")
        }
    }
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

graalvmNative {
    binaries {
        all {
            buildArgs.add("--initialize-at-run-time=ai.onnxruntime.OnnxRuntime")
            buildArgs.add("--initialize-at-run-time=ai.onnxruntime.OrtEnvironment")
            buildArgs.add("--initialize-at-run-time=ai.djl.huggingface.tokenizers.jni.LibUtils")
            buildArgs.add("--initialize-at-run-time=ai.djl.huggingface.tokenizers.jni.TokenizersLibrary")
            buildArgs.add("--initialize-at-run-time=dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel")
        }
    }
}
