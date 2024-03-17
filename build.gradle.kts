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
    implementation(libs.springBootStarterSecurity)
    implementation(libs.springSecurityOauth2Jose)
    implementation(libs.springBootStarterWeb)
    implementation(libs.springBootStarterCache)
    implementation(libs.caffeineCache)
    implementation(libs.kafkaStreams)
    implementation(libs.springKafka)
    implementation(libs.springdocOpenapiStarterWebmvcUi)
    implementation(libs.springKafka)
    implementation(libs.googleGuava)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    testImplementation(libs.springBootStarterTest)
    testImplementation(libs.junitJupiterApi)
    testImplementation(libs.junitJupiterEngine)
    testImplementation(libs.junitJupiterParams)
    testImplementation(libs.springBootTestcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainersJunitJupiter)
    testImplementation(libs.testcontainersRedpanda)
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

val defaultTargetDir = layout.buildDirectory.get().asFile.absolutePath + "/resources/main"
val certDir = layout.buildDirectory.get().asFile.absolutePath + "/generated/certs"

tasks.register("createCertDirs") {
    group = "certificate"
    description = "Create directories for certificate generation"
    if (mkdir(certDir).exists()) {
        println("Created $certDir")
    }
    if (mkdir(defaultTargetDir).exists()) {
        println("Created $defaultTargetDir")
    }
}

val generateKeystore by tasks.registering(Exec::class) {
    group = "certificate"
    description = "Generates a keystore with a self-signed certificate in PKCS12 format"
    outputs.dir(certDir)
    dependsOn("createCertDirs")
    commandLine("keytool", "-genkeypair",
        "-alias", "myalias",
        "-keyalg", "RSA",
        "-keysize", "4096",
        "-storetype", "PKCS12",
        "-keystore", "$certDir/keystore.p12",
        "-validity", "3650",
        "-storepass", "changeme",
        "-keypass", "changeme",
        "-dname", "CN=localhost, OU=Test, O=Test, L=Test, ST=Test, C=GB",
        "-noprompt")
    doLast {
        val targetFile = File("$defaultTargetDir/keystore.p12")
        Files.copy(Paths.get("$certDir/keystore.p12"), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        project.properties["certTargetDir"]?.toString()?.let {
            val customTargetFile = File("$it/keystore.p12")
            Files.copy(Paths.get("$certDir/keystore.p12"), customTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

val exportCertificate by tasks.registering(Exec::class) {
    group = "certificate"
    description = "Exports certificate from the keystore"
    dependsOn("generateKeystore")
    commandLine("keytool", "-exportcert",
        "-alias", "myalias",
        "-keystore", "$certDir/keystore.p12",
        "-storepass", "changeme",
        "-file", "$certDir/mycert.cer",
        "-noprompt")
}

val generateTruststore by tasks.registering(Exec::class) {
    group = "certificate"
    description = "Generates a truststore from the keystore"
    outputs.dir(certDir)
    dependsOn("exportCertificate")
    commandLine("keytool", "-importcert",
        "-file", "$certDir/mycert.cer",
        "-alias", "myTrustAlias",
        "-keystore", "$certDir/truststore.p12",
        "-storepass", "changeme",
        "-noprompt")
    doLast {
        val targetFile = File("$defaultTargetDir/truststore.p12")
        Files.copy(Paths.get("$certDir/truststore.p12"), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        project.properties["certTargetDir"]?.toString()?.let {
            val customTargetFile = File("$it/truststore.p12")
            Files.copy(Paths.get("$certDir/truststore.p12"), customTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

val addKeystoreToUnifiedStore by tasks.registering(Exec::class) {
    group = "certificate"
    description = "Adds the Keystore to the Unified store"
    outputs.dir(certDir)
    dependsOn("generateTruststore")
    commandLine("keytool", "-importkeystore",
        "-srckeystore", "$certDir/keystore.p12",
        "-destkeystore", "$certDir/unified.p12",
        "-srcstoretype", "PKCS12",
        "-deststoretype", "PKCS12",
        "-srcstorepass", "changeme",
        "-deststorepass", "changeme",
        "-noprompt")
}

val addTrustStoreToUnifiedStore by tasks.registering(Exec::class) {
    group = "certificate"
    description = "Adds the Truststore to the Unified store"
    outputs.dir(certDir)
    dependsOn("addKeystoreToUnifiedStore")
    commandLine("keytool", "-importkeystore",
        "-srckeystore", "$certDir/truststore.p12",
        "-destkeystore", "$certDir/unified.p12",
        "-srcstoretype", "PKCS12",
        "-deststoretype", "PKCS12",
        "-srcstorepass", "changeme",
        "-deststorepass", "changeme",
        "-noprompt")
    doLast {
        val targetFile = File("$defaultTargetDir/unified.p12")
        Files.copy(Paths.get("$certDir/unified.p12"), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        project.properties["certTargetDir"]?.toString()?.let {
            val customTargetFile = File("$it/unified.p12")
            Files.copy(Paths.get("$certDir/unified.p12"), customTargetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

tasks.register("createStores") {
    group = "certificate"
    description = "Creates a keystore and truststore and a unified store"
    dependsOn("addTrustStoreToUnifiedStore")
}

tasks.named("bootJar") {
    dependsOn("createStores")
}

tasks.named("bootRun") {
    dependsOn("createStores")
}

tasks.withType(JavaExec::class) {
    jvmArgs("-Djavax.net.debug=ssl:handshake:verbose:keymanager:trustmanager", "-Djava.security.debug=access:stack")
}
