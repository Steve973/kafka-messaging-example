import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission

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

    onlyIf {
        val caCertFile = File("$certDir/ca.crt")
        val serverCertFile = File("$certDir/server.crt")
        val keystoreFile = File("$certDir/keystore.p12")
        val truststoreFile = File("$certDir/truststore.p12")
        !caCertFile.exists() || !serverCertFile.exists() || !keystoreFile.exists() || !truststoreFile.exists()
    }

    outputs.dirs(certDir, defaultTargetDir)

    doLast {
        project.exec {
            isIgnoreExitValue = true
            commandLine(
                "openssl", "req",
                "-new",
                "-newkey", "rsa:4096",
                "-days", "3650",
                "-nodes",
                "-x509",
                "-subj", "/C=US/ST=Test/O=Test/OU=Test/CN=localhost",
                "-keyout", "$certDir/ca.key",
                "-out", "$certDir/ca.crt"
            )
        }

        project.exec {
            isIgnoreExitValue = true
            commandLine(
                "openssl", "genrsa",
                "-out", "$certDir/server.key", "4096"
            )
        }

        project.exec {
            isIgnoreExitValue = true
            commandLine(
                "openssl", "req",
                "-verbose",
                "-new",
                "-config", "./project-resources/certs/certs.cnf",
                "-key", "$certDir/server.key",
                "-out", "$certDir/server.csr"
            )
        }

        project.exec {
            isIgnoreExitValue = true
            commandLine(
                "openssl", "x509",
                "-req",
                "-sha384",
                "-days", "3650",
                "-in", "$certDir/server.csr",
                "-CA", "$certDir/ca.crt",
                "-CAkey", "$certDir/ca.key",
                "-set_serial", "01",
                "-extfile", "./project-resources/certs/certs.cnf", // Add this line
                "-extensions", "v3_req",
                "-out", "$certDir/server.crt"
            )
        }

        project.exec {
            isIgnoreExitValue = true
            commandLine(
                "openssl", "pkcs12",
                "-export",
                "-in", "$certDir/server.crt",
                "-inkey", "$certDir/server.key",
                "-name", "server",
                "-out", "$certDir/keystore.p12",
                "-passout", "pass:changeme",
                "-certfile", "$certDir/ca.crt"
            )
        }

        project.exec {
            isIgnoreExitValue = true
            commandLine(
                "openssl", "pkcs12",
                "-export",
                "-nokeys",
                "-in", "$certDir/ca.crt",
                "-out", "$certDir/truststore.p12",
                "-passout", "pass:changeme",
            )
        }

        Files.newDirectoryStream(Paths.get(certDir)).use { dirStream ->
            dirStream.forEach { filePath ->
                val customTargetFile = File("$defaultTargetDir/${filePath.fileName}")
                Files.copy(
                    filePath,
                    customTargetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING)
                Files.setPosixFilePermissions(customTargetFile.toPath(), setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.OTHERS_READ))
            }
        }

        project.properties["certTargetDir"]?.toString()?.let { targetDir ->
            Files.newDirectoryStream(Paths.get(certDir)).use { dirStream ->
                dirStream.forEach { filePath ->
                    val customTargetFile = File("$targetDir/${filePath.fileName}")
                    Files.copy(
                        filePath,
                        customTargetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING)
                    Files.setPosixFilePermissions(customTargetFile.toPath(), setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.OTHERS_READ))
                }
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
