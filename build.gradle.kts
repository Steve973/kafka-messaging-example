import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

plugins {
    java
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "org.storck"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    create("itest") {
        java.srcDir("src/itest/java")
        resources.srcDir("src/itest/resources")
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
        runtimeClasspath += output + compileClasspath
    }
}

tasks.named<Copy>("processItestResources").configure {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.caffeine.cache)
    implementation(libs.google.guava)
    implementation(libs.kafka.streams)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.kafka)
    implementation(libs.spring.kafka)
    implementation(libs.spring.security.oauth2.jose)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.redpanda)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val itest by tasks.registering(Test::class) {
    group = "verification"
    description = "Runs the integration tests."
    testClassesDirs = sourceSets.named("itest").get().output.classesDirs
    classpath = sourceSets.named("itest").get().runtimeClasspath
    dependsOn("itestClasses")
}

val createStores by tasks.registering {
    group = "certificate"
    description = "Creates a keystore and truststore and a unified store"

    val buildDirPath = layout.buildDirectory.get().asFile.absolutePath
    val certDir = "$buildDirPath/generated/certs"
    val defaultTargetDir = "$buildDirPath/resources/main"
    outputs.dirs(certDir, defaultTargetDir)

    doLast {
        project.exec {
            isIgnoreExitValue = true
            commandLine(
                "keytool", "-genkeypair",
                "-alias", "myalias",
                "-keyalg", "RSA",
                "-keysize", "4096",
                "-storetype", "PKCS12",
                "-keystore", "$certDir/keystore.p12",
                "-validity", "3650",
                "-storepass", "changeme",
                "-keypass", "changeme",
                "-dname", "CN=localhost, OU=Test, O=Test, L=Test, ST=Test, C=GB",
                "-noprompt"
            )
        }

        project.exec {
            isIgnoreExitValue = true
            commandLine(
                "keytool", "-exportcert",
                "-alias", "myalias",
                "-keystore", "$certDir/keystore.p12",
                "-storepass", "changeme",
                "-file", "$certDir/mycert.cer",
                "-noprompt"
            )
        }

        project.exec {
            isIgnoreExitValue = true
            commandLine(
                "keytool", "-importcert",
                "-file", "$certDir/mycert.cer",
                "-alias", "myTrustAlias",
                "-keystore", "$certDir/truststore.p12",
                "-storepass", "changeme",
                "-noprompt"
            )
        }

        listOf("keystore.p12", "truststore.p12").forEach { storeName ->
            val defaultTargetFile = File("$defaultTargetDir/$storeName")
            Files.copy(
                Paths.get("$certDir/$storeName"),
                defaultTargetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        project.properties["certTargetDir"]?.toString()?.let { targetDir ->
            listOf("keystore.p12", "truststore.p12").forEach { storeName ->
                val customTargetFile = File("$targetDir/$storeName")
                Files.copy(
                    Paths.get("$certDir/$storeName"),
                    customTargetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }
}

tasks.getByName<BootJar>("bootJar") {
    dependsOn("createStores")
    layered {
        enabled.set(true)
        includeLayerTools.set(true)
    }
}

tasks.getByName<Task>("resolveMainClassName") {
    mustRunAfter("createStores")
}

tasks.getByName<BootBuildImage>("bootBuildImage") {
    imageName.set("docker.io/library/${project.name}")
    environment.set(
        environment.get() +
                mapOf(
                    "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
                    "BPE_APPEND_JAVA_TOOL_OPTIONS" to "--add-opens=java.base/sun.net=ALL-UNNAMED",
                    "BP_HEALTH_CHECKER_ENABLED" to "true",
                    "THC_PATH" to "/actuator/health"
                )
    )
    buildpacks.set(
        listOf(
            "urn:cnb:builder:paketo-buildpacks/java",
            "gcr.io/paketo-buildpacks/health-checker:latest"
        )
    )
}
